package ir.ebb.wallet.wallet;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.service.query.WalletQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;

import java.time.Duration;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/**
 * Facade over the sharded, event-sourced {@link WalletEntity} (one entity per account, located
 * exclusively via Cluster Sharding — there is no actor registry).
 *
 * <p>Single-wallet mutations use the ask pattern against the entity (entity id = accountNumber)
 * and map the {@link WalletReply} to a {@link Wallet} or throw a {@link BusinessException}.
 * Reads ({@code getWallet}, {@code getBuyingPower}) bypass the entity and read the projection-fed
 * tables via {@link WalletQueryService}. Batch separ-credit / Rayan-reconcile / legacy-seed
 * operations are <em>fire-and-forget</em> (one {@code tell} per wallet).
 */
@Slf4j
public class WalletFacade {

    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(10);

    private final ActorSystem<?> system;
    private final ClusterSharding sharding;
    private final WalletQueryService walletQueryService;

    public WalletFacade(ActorSystem<?> system, WalletQueryService walletQueryService) {
        this.system = system;
        this.sharding = ClusterSharding.get(system);
        this.walletQueryService = walletQueryService;
    }

    // ── reads (projection read-model) ──────────────────────────────────────────

    public Wallet getWallet(User user) {
        return walletQueryService.getWallet(user.getDbsAccountNumber());
    }

    public BuyingPower getBuyingPower(User user, SettlementDelay settlementDelay) {
        return walletQueryService.getBuyingPower(user.getDbsAccountNumber(), settlementDelay);
    }

    // ── single-wallet mutations (ask → entity) ─────────────────────────────────

    public Wallet deposit(UUID trackingId, User user, long amount,
                          SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(user, replyTo -> new WalletCommand.Deposit(trackingId, user, amount, settlementDelay, type, replyTo));
    }

    public Wallet withdraw(UUID trackingId, User user, long amount,
                            SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(user, replyTo -> new WalletCommand.Withdraw(trackingId, user, amount, settlementDelay, type, replyTo));
    }

    public Wallet freeze(UUID trackingId, User user, long amount,
                          SettlementDelay settlementDelay, WalletTransactionType type, boolean canSpendSeparCredit) {
        return ask(user, replyTo -> new WalletCommand.Freeze(trackingId, user, amount, settlementDelay, type, canSpendSeparCredit, replyTo));
    }

    public Wallet unfreeze(UUID trackingId, User user, long amount,
                            SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(user, replyTo -> new WalletCommand.Unfreeze(trackingId, user, amount, settlementDelay, type, replyTo));
    }

    public Wallet spend(UUID trackingId, User user, long amount,
                         SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(user, replyTo -> new WalletCommand.Spend(trackingId, user, amount, settlementDelay, type, replyTo));
    }

    public Wallet freezeForT0(UUID trackingId, User user, long amount, WalletTransactionType type) {
        return ask(user, replyTo -> new WalletCommand.FreezeForT0(trackingId, user, amount, type, replyTo));
    }

    public Wallet spendT0(UUID trackingId, User user, long amount, WalletTransactionType type) {
        return ask(user, replyTo -> new WalletCommand.SpendT0(trackingId, user, amount, type, replyTo));
    }

    public Wallet addCredit(UUID trackingId, User user, long creditAmount) {
        return ask(user, replyTo -> new WalletCommand.AddCredit(trackingId, user, creditAmount, replyTo));
    }

    /** Create an empty wallet for a user (admin). */
    public Wallet createWallet(UUID walletId, User user) {
        return ask(user, replyTo -> new WalletCommand.CreateWallet(user, walletId, replyTo));
    }

    /** Reconcile a wallet from the authoritative Rayan snapshot (target state computed by the caller). */
    public void reconcileFromRayan(User user, WalletState target) {
        entityRef(user).tell(new WalletCommand.ReconcileFromRayan(target));
    }

    // ── batch separ-credit operations (fire-and-forget) ────────────────────────

    public void chargeSeparCredits(HashMap<User, Long> userAmounts, boolean isLoan) {
        userAmounts.forEach((user, amount) ->
                entityRef(user).tell(new WalletCommand.ChargeSeparCredit(user, amount, isLoan)));
    }

    public void settleSeparCredits(Set<User> users) {
        users.forEach(user ->
                entityRef(user).tell(new WalletCommand.SettleSeparCredit(user)));
    }

    // ── one-off migration seed (fire-and-forget, idempotent) ───────────────────

    public void seedFromLegacy(WalletState state) {
        entityRef(state.accountNumber())
                .tell(new WalletCommand.SeedFromLegacy(state));
    }

    // ── ask helper ─────────────────────────────────────────────────────────────

    private Wallet ask(User user, Function<ActorRef<WalletReply>, WalletCommand> cmdFactory) {
        WalletReply reply;
        try {
            reply = AskPattern.<WalletCommand, WalletReply>ask(
                            entityRef(user),
                            cmdFactory::apply,
                            ASK_TIMEOUT,
                            system.scheduler())
                    .toCompletableFuture().join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Wallet entity ask timed out or failed for account {}", user.getDbsAccountNumber(), cause);
            throw new BusinessException("Wallet actor unavailable: " + cause.getMessage(),
                    ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
        }
        return switch (reply) {
            case WalletReply.Accepted acc -> acc.state().toAggregate();
            case WalletReply.Rejected rej -> throw new BusinessException(rej.message(), rej.code());
        };
    }

    private org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef<WalletCommand> entityRef(User user) {
        return entityRef(user.getDbsAccountNumber());
    }

    private org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef<WalletCommand> entityRef(long accountNumber) {
        return sharding.entityRefFor(WalletEntity.ENTITY_TYPE_KEY, String.valueOf(accountNumber));
    }
}
