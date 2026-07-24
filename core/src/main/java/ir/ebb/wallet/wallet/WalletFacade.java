package ir.ebb.wallet.wallet;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/**
 * Facade over the sharded, event-sourced {@link WalletActor} (one entity per account, located
 * exclusively via Cluster Sharding — there is no actor registry).
 *
 * <p>Single-wallet mutations use the ask pattern against the entity (entity id = accountNumber)
 * and map the {@link WalletReply} to a {@link Wallet} or throw a {@link BusinessException}.
 * Single-wallet reads ({@code getWallet}, {@code getBuyingPower}) also ask the entity — its
 * in-memory {@link WalletState} is the authoritative current state (the journal is the source of
 * truth), so reads are strongly consistent. Batch separ-credit / Rayan-reconcile / legacy-seed
 * operations are <em>fire-and-forget</em> (one {@code tell} per wallet).
 */
@Slf4j
public class WalletFacade {

    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(10);

    private final ActorSystem<?> system;
    private final ClusterSharding sharding;

    public WalletFacade(ActorSystem<?> system) {
        this.system = system;
        this.sharding = ClusterSharding.get(system);
    }

    // ── reads (authoritative state from the entity) ────────────────────────────

//    public Wallet getWallet(User user) {
//        return getWallet(user.getDbsAccountNumber());
//    }

    public Wallet getWallet(long accountNumber) {
        WalletReply reply = askRaw(accountNumber, WalletCommand.GetWallet::new);
        return switch (reply) {
            case WalletReply.WalletSnapshot snap -> snap.state().toAggregate();
            case WalletReply.Rejected rej -> throw new BusinessException(rej.message(), rej.code());
            default -> throw new IllegalStateException("Unexpected wallet read reply: " + reply);
        };
    }

//    public BuyingPower getBuyingPower(long accountNumber, SettlementDelay settlementDelay) {
//        return getBuyingPower(user.getDbsAccountNumber(), settlementDelay);
//    }

    public BuyingPower getBuyingPower(long accountNumber, SettlementDelay settlementDelay) {
        WalletReply reply = askRaw(accountNumber,
                replyTo -> new WalletCommand.GetBuyingPower(settlementDelay, replyTo));
        return switch (reply) {
            case WalletReply.BuyingPowerResult bp -> bp.buyingPower();
            case WalletReply.Rejected rej -> throw new BusinessException(rej.message(), rej.code());
            default -> throw new IllegalStateException("Unexpected buying-power read reply: " + reply);
        };
    }

    // ── single-wallet mutations (ask → entity) ─────────────────────────────────

    public Wallet deposit(UUID trackingId, long accountNumber, long amount,
                          SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(accountNumber, replyTo -> new WalletCommand.Deposit(trackingId, accountNumber, amount, settlementDelay, type, replyTo));
    }

    public Wallet withdraw(UUID trackingId, long accountNumber, long amount,
                            SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(accountNumber, replyTo -> new WalletCommand.Withdraw(trackingId, accountNumber, amount, settlementDelay, type, replyTo));
    }

    public Wallet freeze(UUID trackingId, long accountNumber, long amount,
                          SettlementDelay settlementDelay, WalletTransactionType type, boolean canSpendSeparCredit) {
        return ask(accountNumber, replyTo -> new WalletCommand.Freeze(trackingId, accountNumber, amount, settlementDelay, type, canSpendSeparCredit, replyTo));
    }

    public Wallet unfreeze(UUID trackingId, long accountNumber, long amount,
                            SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(accountNumber, replyTo -> new WalletCommand.Unfreeze(trackingId, accountNumber, amount, settlementDelay, type, replyTo));
    }

    public Wallet spend(UUID trackingId, long accountNumber, long amount,
                         SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(accountNumber, replyTo -> new WalletCommand.Spend(trackingId, accountNumber, amount, settlementDelay, type, replyTo));
    }

    public Wallet freezeForT0(UUID trackingId, long accountNumber, long amount, WalletTransactionType type) {
        return ask(accountNumber, replyTo -> new WalletCommand.FreezeForT0(trackingId, accountNumber, amount, type, replyTo));
    }

    public Wallet spendT0(UUID trackingId, long accountNumber, long amount, WalletTransactionType type) {
        return ask(accountNumber, replyTo -> new WalletCommand.SpendT0(trackingId, accountNumber, amount, type, replyTo));
    }

    public Wallet addCredit(UUID trackingId, long accountNumber, long creditAmount) {
        return ask(accountNumber, replyTo -> new WalletCommand.AddCredit(trackingId, accountNumber, creditAmount, replyTo));
    }

    /** Create an empty wallet for an account (admin). {@code walletId} is generated by the caller. */
    public Wallet createWallet(long accountNumber, UUID walletId) {
        return ask(accountNumber, replyTo -> new WalletCommand.CreateWallet(accountNumber, walletId, replyTo));
    }

    /** Reconcile a wallet from the authoritative Rayan snapshot (target state computed by the caller; fire-and-forget). */
    public void reconcileFromRayan(WalletState target) {
        entityRef(target.accountNumber()).tell(new WalletCommand.ReconcileFromRayan(target));
    }

    // ── batch separ-credit operations (fire-and-forget) ────────────────────────

//    public void chargeSeparCredits(HashMap<User, Long> userAmounts, boolean isLoan) {
//        userAmounts.forEach((user, amount) ->
//                entityRef(user).tell(new WalletCommand.ChargeSeparCredit(user, amount, isLoan)));
//    }

//    public void settleSeparCredits(Set<User> users) {
//        users.forEach(user ->
//                entityRef(user).tell(new WalletCommand.SettleSeparCredit(user)));
//    }

    // ── one-off migration seed (fire-and-forget, idempotent) ───────────────────

    public void seedFromLegacy(WalletState state) {
        entityRef(state.accountNumber())
                .tell(new WalletCommand.SeedFromLegacy(state));
    }

    // ── ask helper ─────────────────────────────────────────────────────────────

    /** Send a command to the entity and await its reply; maps ask failure/timeout to a BusinessException. */
    private WalletReply askRaw(long accountNumber, Function<ActorRef<WalletReply>, WalletCommand> cmdFactory) {
        try {
            return AskPattern.ask(
                            entityRef(accountNumber),
                            cmdFactory::apply,
                            ASK_TIMEOUT,
                            system.scheduler())
                    .toCompletableFuture().join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Wallet entity ask timed out or failed for account {}", accountNumber, cause);
            throw new BusinessException("Wallet actor unavailable: " + cause.getMessage(),
                    ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    private Wallet ask(long accountNumber, Function<ActorRef<WalletReply>, WalletCommand> cmdFactory) {
        WalletReply reply = askRaw(accountNumber, cmdFactory);
        return switch (reply) {
            case WalletReply.Accepted acc -> acc.state().toAggregate();
            case WalletReply.Rejected rej -> throw new BusinessException(rej.message(), rej.code());
            default -> throw new IllegalStateException("Unexpected wallet mutation reply: " + reply);
        };
    }

    private EntityRef<WalletCommand> entityRef(long accountNumber) {
        return sharding.entityRefFor(WalletActor.ENTITY_TYPE_KEY, String.valueOf(accountNumber));
    }
}
