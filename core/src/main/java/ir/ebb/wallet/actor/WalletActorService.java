package ir.ebb.wallet.actor;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.base.exception.OptimisticLockingFailureException;
import ir.ebb.base.jdbc.JdbcException;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.actor.message.WalletCommand;
import ir.ebb.wallet.actor.message.WalletEvent;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.service.command.WalletCommandService;
import ir.ebb.wallet.service.query.WalletQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/**
 * Facade that routes all wallet mutations through the Pekka actor system.
 * Each method sends a typed message to the registry, which dispatches it to
 * the correct DailyWalletActor. Mutations are serialised per-user via the
 * actor's single mailbox.
 *
 * Reads (getBuyingPower) bypass the actor and go directly to the read-only
 * query service — the DB is always consistent after every actor-committed save.
 *
 * spendAndTransfer involves two wallets and cannot be made atomic inside two
 * separate actor mailboxes; it delegates to WalletCommandService directly.
 * If either actor has the affected wallet cached, the next save will detect
 * the version mismatch and reload automatically.
 */
@Slf4j
@RequiredArgsConstructor
public class WalletActorService {

    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(10);

    private final ActorSystem<WalletRegistryActor.Command> actorSystem;
    private final WalletQueryService walletQueryService;
    private final WalletCommandService walletCommandService;

    // ── queries ───────────────────────────────────────────────────────────────

    public Wallet getWallet(User user) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(
                        user.getDbsAccountNumber(),
                        new WalletCommand.GetWallet(user, replyTo)));
    }

    public BuyingPower getBuyingPower(User user, SettlementDelay settlementDelay) {
        return walletQueryService.getBuyingPower(user.getDbsAccountNumber(), settlementDelay);
    }

    // ── single-wallet mutations ───────────────────────────────────────────────

    public Wallet deposit(UUID trackingId, User user, long amount,
                          SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(user.getDbsAccountNumber(),
                        new WalletCommand.Deposit(trackingId, user, amount, settlementDelay, type, replyTo)));
    }

    public Wallet withdraw(UUID trackingId, User user, long amount,
                           SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(user.getDbsAccountNumber(),
                        new WalletCommand.Withdraw(trackingId, user, amount, settlementDelay, type, replyTo)));
    }

    public Wallet freeze(UUID trackingId, User user, long amount,
                         SettlementDelay settlementDelay, WalletTransactionType type,
                         boolean canSpendSeparCredit) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(user.getDbsAccountNumber(),
                        new WalletCommand.Freeze(trackingId, user, amount, settlementDelay, type, canSpendSeparCredit, replyTo)));
    }

    public Wallet unfreeze(UUID trackingId, User user, long amount,
                           SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(user.getDbsAccountNumber(),
                        new WalletCommand.Unfreeze(trackingId, user, amount, settlementDelay, type, replyTo)));
    }

    public Wallet spend(UUID trackingId, User user, long amount,
                        SettlementDelay settlementDelay, WalletTransactionType type) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(user.getDbsAccountNumber(),
                        new WalletCommand.Spend(trackingId, user, amount, settlementDelay, type, replyTo)));
    }

    public Wallet freezeForT0(UUID trackingId, User user, long amount, WalletTransactionType type) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(user.getDbsAccountNumber(),
                        new WalletCommand.FreezeForT0(trackingId, user, amount, type, replyTo)));
    }

    public Wallet spendT0(UUID trackingId, User user, long amount, WalletTransactionType type) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(user.getDbsAccountNumber(),
                        new WalletCommand.SpendT0(trackingId, user, amount, type, replyTo)));
    }

    public Wallet addCredit(UUID trackingId, User user, long creditAmount) {
        return ask(user.getDbsAccountNumber(), replyTo ->
                new WalletRegistryActor.Command.Route(user.getDbsAccountNumber(),
                        new WalletCommand.AddCredit(trackingId, user, creditAmount, replyTo)));
    }

    // ── cross-wallet / batch ──────────────────────────────────────────────────

    /**
     * Atomically spends from one wallet and deposits to another.
     * Uses a DB-level transaction so both wallets are updated together.
     * If either wallet is cached in an actor, the actor will detect the version
     * change on its next save and reload automatically.
     */
    public void spendAndTransfer(UUID trackingId, User fromUser, User toUser,
                                 long amount, SettlementDelay settlementDelay, WalletTransactionType type) {
        try {
            walletCommandService.spendAndTransfer(trackingId, fromUser, toUser, amount, settlementDelay, type);
        } catch (ApplicationException e) {
            throw new BusinessException(e.getMessage(), 4005);
        } catch (OptimisticLockingFailureException | JdbcException e) {
            // Optimistic-lock conflict and other DB errors from the direct-TX path
            throw new BusinessException(e.getMessage(), ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    /**
     * Routes a separ-credit charge to every affected user's actor so the
     * in-memory wallet stays consistent. All N requests are sent in parallel;
     * per-user failures are logged and do not abort the others.
     */
    public void chargeSeparCredits(HashMap<User, Long> userCredits, boolean isLoan) {
        List<CompletableFuture<WalletEvent>> futures = userCredits.entrySet().stream()
                .map(e -> AskPattern.<WalletRegistryActor.Command, WalletEvent>ask(
                        actorSystem,
                        replyTo -> new WalletRegistryActor.Command.Route(
                                e.getKey().getDbsAccountNumber(),
                                new WalletCommand.ChargeSeparCredit(e.getKey(), e.getValue(), isLoan, replyTo)),
                        ASK_TIMEOUT,
                        actorSystem.scheduler()
                ).toCompletableFuture())
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .exceptionally(ex -> {
                    log.error("chargeSeparCredits actor timeout or failure", ex);
                    return null;
                })
                .join();

        logOperationFailures("chargeSeparCredits", futures);
    }

    /**
     * Routes a separ-credit settlement to every affected user's actor.
     * Parallel dispatch; per-user failures are logged but do not abort siblings.
     */
    public void settleSeparCredits(Set<User> users) {
        List<CompletableFuture<WalletEvent>> futures = users.stream()
                .map(user -> AskPattern.<WalletRegistryActor.Command, WalletEvent>ask(
                        actorSystem,
                        replyTo -> new WalletRegistryActor.Command.Route(
                                user.getDbsAccountNumber(),
                                new WalletCommand.SettleSeparCredit(user, replyTo)),
                        ASK_TIMEOUT,
                        actorSystem.scheduler()
                ).toCompletableFuture())
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .exceptionally(ex -> {
                    log.error("settleSeparCredits actor timeout or failure", ex);
                    return null;
                })
                .join();

        logOperationFailures("settleSeparCredits", futures);
    }

    // ── ask helper ────────────────────────────────────────────────────────────

    private Wallet ask(long accountNumber,
                       Function<ActorRef<WalletEvent>, WalletRegistryActor.Command> cmdFactory) {
        WalletEvent event;
        try {
            event = AskPattern.<WalletRegistryActor.Command, WalletEvent>ask(
                    actorSystem,
                    cmdFactory::apply,
                    ASK_TIMEOUT,
                    actorSystem.scheduler()
            ).toCompletableFuture().join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            log.error("Wallet actor ask timed out or failed for account {}", accountNumber, cause);
            String msg = cause != null ? cause.getMessage() : e.getMessage();
            // Use INTERNAL_SERVER_ERROR (5000) so callers can distinguish infrastructure
            // failures from domain errors (which arrive as WalletEvent.OperationFailed).
            throw new BusinessException("Wallet actor unavailable: " + msg,
                    ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
        }
        return switch (event) {
            case WalletEvent.WalletUpdated updated -> updated.wallet();
            case WalletEvent.OperationFailed failed -> throw new BusinessException(failed.message(), 4005);
        };
    }

    /** Logs any WalletEvent.OperationFailed results from a fan-out batch of futures. */
    private void logOperationFailures(String operation, List<CompletableFuture<WalletEvent>> futures) {
        List<String> failures = new ArrayList<>();
        for (CompletableFuture<WalletEvent> f : futures) {
            if (f.isDone() && !f.isCompletedExceptionally()) {
                WalletEvent event = f.getNow(null);
                if (event instanceof WalletEvent.OperationFailed failed) {
                    failures.add(failed.message());
                }
            }
        }
        if (!failures.isEmpty()) {
            log.error("{} had {} per-user failures: {}", operation, failures.size(), failures);
        }
    }
}
