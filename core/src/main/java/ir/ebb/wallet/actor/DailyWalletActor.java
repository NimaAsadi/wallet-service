package ir.ebb.wallet.actor;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.actor.message.WalletCommand;
import ir.ebb.wallet.actor.message.WalletEvent;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.service.command.WalletCommandService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.apache.pekko.actor.typed.javadsl.TimerScheduler;
import ir.ebb.base.exception.OptimisticLockingFailureException;

import java.time.Duration;

/**
 * Stateful actor representing one user's wallet for a single calendar day.
 * Key = accountNumber_date (managed by WalletRegistryActor).
 *
 * The wallet aggregate is loaded on the first message and kept in memory.
 * All mutations are applied in-memory then flushed to the DB via
 * WalletCommandService.save(), which does a version-checked native UPDATE.
 * On an OptimisticLockingFailureException (cross-JVM conflict), the actor
 * reloads from DB and retries the mutation once.
 *
 * The actor runs on wallet-blocking-dispatcher (fixed thread-pool) so blocking
 * JDBC calls are safe and do not starve the default fork-join scheduler.
 *
 * Passivation: a single-shot timer restarts on every message. After
 * PASSIVATION_TIMEOUT of inactivity the actor sends Deregistering to the
 * registry (so the registry removes the key immediately) then stops itself.
 */
@Slf4j
public class DailyWalletActor extends AbstractBehavior<WalletCommand> {

    private static final Duration PASSIVATION_TIMEOUT = Duration.ofMinutes(30);
    private static final Object TIMER_KEY = new Object();

    private final TimerScheduler<WalletCommand> timers;
    private final WalletCommandService commandService;
    private final ActorRef<WalletRegistryActor.Command> registry;
    private final String key;

    private Wallet wallet; // null until first message

    // ── factory ──────────────────────────────────────────────────────────────

    public static Behavior<WalletCommand> create(WalletCommandService commandService,
                                                  ActorRef<WalletRegistryActor.Command> registry,
                                                  String key) {
        return Behaviors.withTimers(timers ->
                Behaviors.setup(ctx -> new DailyWalletActor(ctx, timers, commandService, registry, key)));
    }

    private DailyWalletActor(ActorContext<WalletCommand> ctx,
                              TimerScheduler<WalletCommand> timers,
                              WalletCommandService commandService,
                              ActorRef<WalletRegistryActor.Command> registry,
                              String key) {
        super(ctx);
        this.timers = timers;
        this.commandService = commandService;
        this.registry = registry;
        this.key = key;
        restartTimer();
    }

    // ── message dispatch ──────────────────────────────────────────────────────

    @Override
    public Receive<WalletCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WalletCommand.GetWallet.class,         this::onGetWallet)
                .onMessage(WalletCommand.Deposit.class,           this::onDeposit)
                .onMessage(WalletCommand.Withdraw.class,          this::onWithdraw)
                .onMessage(WalletCommand.Freeze.class,            this::onFreeze)
                .onMessage(WalletCommand.Unfreeze.class,          this::onUnfreeze)
                .onMessage(WalletCommand.Spend.class,             this::onSpend)
                .onMessage(WalletCommand.FreezeForT0.class,       this::onFreezeForT0)
                .onMessage(WalletCommand.SpendT0.class,           this::onSpendT0)
                .onMessage(WalletCommand.AddCredit.class,         this::onAddCredit)
                .onMessage(WalletCommand.ChargeSeparCredit.class, this::onChargeSeparCredit)
                .onMessage(WalletCommand.SettleSeparCredit.class, this::onSettleSeparCredit)
                .onMessage(WalletCommand.Passivate.class,         this::onPassivate)
                .build();
    }

    // ── query ─────────────────────────────────────────────────────────────────

    private Behavior<WalletCommand> onGetWallet(WalletCommand.GetWallet msg) {
        try {
            ensureLoaded(msg.user());
            msg.replyTo().tell(new WalletEvent.WalletUpdated(wallet));
        } catch (Exception e) {
            msg.replyTo().tell(new WalletEvent.OperationFailed(e.getMessage()));
        }
        restartTimer();
        return this;
    }

    // ── mutations ─────────────────────────────────────────────────────────────

    private Behavior<WalletCommand> onDeposit(WalletCommand.Deposit msg) {
        return runMutation(msg.user(), msg.replyTo(), () ->
                wallet.deposit(msg.trackingId(), new Money(msg.amount()), msg.settlementDelay(), msg.type()));
    }

    private Behavior<WalletCommand> onWithdraw(WalletCommand.Withdraw msg) {
        return runMutation(msg.user(), msg.replyTo(), () ->
                wallet.withdraw(msg.trackingId(), new Money(msg.amount()), msg.settlementDelay(), msg.type()));
    }

    private Behavior<WalletCommand> onFreeze(WalletCommand.Freeze msg) {
        return runMutation(msg.user(), msg.replyTo(), () ->
                wallet.freeze(msg.trackingId(), new Money(msg.amount()), msg.settlementDelay(), msg.type(), msg.canSpendSeparCredit()));
    }

    private Behavior<WalletCommand> onUnfreeze(WalletCommand.Unfreeze msg) {
        return runMutation(msg.user(), msg.replyTo(), () ->
                wallet.unfreeze(msg.trackingId(), new Money(msg.amount()), msg.settlementDelay(), msg.type()));
    }

    private Behavior<WalletCommand> onSpend(WalletCommand.Spend msg) {
        return runMutation(msg.user(), msg.replyTo(), () ->
                wallet.spend(msg.trackingId(), new Money(msg.amount()), msg.settlementDelay(), msg.type()));
    }

    private Behavior<WalletCommand> onFreezeForT0(WalletCommand.FreezeForT0 msg) {
        return runMutation(msg.user(), msg.replyTo(), () ->
                wallet.freeze(msg.trackingId(), new Money(msg.amount()), SettlementDelay.T_PLUS_0, msg.type(), false));
    }

    private Behavior<WalletCommand> onSpendT0(WalletCommand.SpendT0 msg) {
        return runMutation(msg.user(), msg.replyTo(), () ->
                wallet.spend(msg.trackingId(), new Money(msg.amount()), SettlementDelay.T_PLUS_0, msg.type()));
    }

    private Behavior<WalletCommand> onAddCredit(WalletCommand.AddCredit msg) {
        return runMutation(msg.user(), msg.replyTo(), () ->
                wallet.addCredit(msg.trackingId(), new Money(msg.creditAmount())));
    }

    private Behavior<WalletCommand> onChargeSeparCredit(WalletCommand.ChargeSeparCredit msg) {
        return runMutation(msg.user(), msg.replyTo(), () -> {
            if (msg.isLoan()) {
                wallet.setSeparInitialCredit(msg.amount());
                wallet.setSeparCredit(msg.amount());
            } else {
                wallet.decreaseSeparCredit(msg.amount());
            }
        });
    }

    private Behavior<WalletCommand> onSettleSeparCredit(WalletCommand.SettleSeparCredit msg) {
        // Force reload from DB so settlement uses the authoritative separCredit/separInitialCredit
        // values, not potentially stale in-memory ones (e.g., after cross-JVM freeze).
        wallet = null;
        return runMutation(msg.user(), msg.replyTo(), () -> {
            wallet.setSeparCredit(wallet.getSeparCredit() - wallet.getSeparInitialCredit());
            wallet.setSeparInitialCredit(0L);
        });
    }

    // ── passivation ───────────────────────────────────────────────────────────

    private Behavior<WalletCommand> onPassivate(WalletCommand.Passivate msg) {
        // Notify the registry to remove this key from its map before we stop.
        // This closes the race window where a concurrent Route message would find
        // a stale (stopping) actor ref in the registry and send a command to dead letters.
        registry.tell(new WalletRegistryActor.Command.Deregistering(key));
        return Behaviors.stopped();
    }

    // ── mutation helper ───────────────────────────────────────────────────────

    /**
     * Loads the wallet on first call, clears pending transactions, executes
     * the mutation lambda, persists, and updates the in-memory version.
     * On an OptimisticLockingFailureException the wallet is reloaded and the
     * mutation retried once before giving up.
     */
    private Behavior<WalletCommand> runMutation(User user, ActorRef<WalletEvent> replyTo, MutationOp mutation) {
        try {
            ensureLoaded(user);
            wallet.getWalletTransactions().clear();
            mutation.run();
            commitWallet();
            replyTo.tell(new WalletEvent.WalletUpdated(wallet));
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict for user {}, reloading and retrying", user.getDbsAccountNumber());
            try {
                wallet = commandService.getWallet(user);
                wallet.getWalletTransactions().clear();
                mutation.run();
                commitWallet();
                replyTo.tell(new WalletEvent.WalletUpdated(wallet));
            } catch (Exception retryEx) {
                wallet = null; // partially-mutated state is unreliable; force reload next time
                replyTo.tell(new WalletEvent.OperationFailed(retryEx.getMessage()));
            }
        } catch (ApplicationException | BusinessException e) {
            wallet = null; // force reload on next op; in-memory state may be partially modified
            replyTo.tell(new WalletEvent.OperationFailed(e.getMessage()));
        } catch (Exception e) {
            wallet = null;
            log.error("Wallet actor error for user {}", user.getDbsAccountNumber(), e);
            replyTo.tell(new WalletEvent.OperationFailed(e.getMessage()));
        }
        restartTimer();
        return this;
    }

    private void commitWallet() {
        commandService.save(wallet);
        wallet.setVersion(wallet.getVersion() + 1); // mirror DB's version = version + 1
        wallet.getWalletTransactions().clear();      // don't re-save on the next mutation
    }

    private void ensureLoaded(User user) {
        if (wallet == null) {
            wallet = commandService.getWallet(user);
        }
    }

    private void restartTimer() {
        timers.startSingleTimer(TIMER_KEY, new WalletCommand.Passivate(), PASSIVATION_TIMEOUT);
    }

    @FunctionalInterface
    private interface MutationOp {
        void run() throws ApplicationException;
    }
}
