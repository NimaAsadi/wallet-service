package ir.ebb.wallet.wallet;

import io.vavr.control.Try;
import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.aggregate.WalletAggregate;
import ir.ebb.wallet.aggregate.WalletTransaction;
import ir.ebb.wallet.constant.valueobject.Money;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityContext;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;
import org.apache.pekko.pattern.StatusReply;
import org.apache.pekko.persistence.typed.PersistenceId;
import org.apache.pekko.persistence.typed.javadsl.CommandHandler;
import org.apache.pekko.persistence.typed.javadsl.Effect;
import org.apache.pekko.persistence.typed.javadsl.EventHandler;
import org.apache.pekko.persistence.typed.javadsl.EventSourcedBehavior;
import org.apache.pekko.persistence.typed.javadsl.RetentionCriteria;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Event-sourced, cluster-sharded {@code Wallet} aggregate.
 *
 * <p>One entity per account number across the cluster ({@link #ENTITY_TYPE_KEY}, entity id =
 * accountNumber). State is reconstructed from the persisted {@link WalletEvent} journal
 * (+ snapshots); the event journal is the single source of truth. The money-movement
 * cascade in {@link Wallet} is reused verbatim: each mutating command rebuilds a transient
 * mutable {@link Wallet} from the current {@link WalletState}, runs the mutation, and
 * persists a {@link WalletEvent.WalletMutated} capturing the audit legs + the resulting state.
 *
 * <p>Idempotency: duplicate {@code trackingId}s (recently seen) are rejected with
 * {@code DUPLICATE_TRACKING_ID}; event application is idempotent by construction
 * (absolute resulting-state values). The journal serializes writes per persistence-id,
 * replacing the old optimistic-lock + reload-and-retry path.
 *
 * <p>Events are tagged with a single {@link WalletTags#TAG} so the slice-based
 * {@code eventsBySlices} R2DBC projections can consume them (partitioning is by entity type
 * + slice range, derived from the persistence id — see {@code ProjectionBootstrap}).
 */
@Slf4j
public class WalletActor extends EventSourcedBehavior<WalletCommand, WalletEvent, WalletState> {

    public static final EntityTypeKey<WalletCommand> ENTITY_TYPE_KEY =
            EntityTypeKey.create(WalletCommand.class, "wallet");

    /** Keep this many recently-applied trackingIds for command idempotency. */
    private static final int SEEN_TRACKING_IDS = 1024;
    /** Snapshot the state every N persisted events to speed up recovery. */
    private static final int SNAPSHOT_EVERY = 100;
    private static final int KEEP_SNAPSHOTS = 2;

    private final long accountNumber;
    /*private final WalletAggregate walletAggregate;*/

    public static Behavior<WalletCommand> create(EntityContext<WalletCommand> ctx) {
        return create(ctx.getEntityId());
    }

    /**
     * Build the wallet entity behavior from a plain entity id (the account number). Used by
     * {@code ClusterSharding.init} via {@link #create(EntityContext)} and by the persistence
     * testkit (which constructs the entity directly, not through sharding).
     */
    public static Behavior<WalletCommand> create(String entityId) {
        long acct = Long.parseLong(entityId);
        PersistenceId pid = PersistenceId.of(ENTITY_TYPE_KEY.name(), entityId);
        return Behaviors.setup(context -> Behaviors.supervise(new WalletActor(pid, acct))
                .onFailure(SupervisorStrategy.restartWithBackoff(
                        Duration.ofSeconds(1), Duration.ofSeconds(10), 0.2)));
    }

    private WalletActor(PersistenceId persistenceId, long accountNumber) {
        super(persistenceId);
        this.accountNumber = accountNumber;
    }

    @Override
    public WalletState emptyState() {
        return WalletState.empty();
    }

    /** Tag every wallet event with the single wallet tag (the R2DBC projection partitions by slice, not tag). */
    @Override
    public Set<String> tagsFor(WalletEvent event) {
        return Set.of(WalletTags.TAG);
    }

    @Override
    public RetentionCriteria retentionCriteria() {
        return RetentionCriteria.snapshotEvery(SNAPSHOT_EVERY, KEEP_SNAPSHOTS);
    }

    // ── command handling ──────────────────────────────────────────────────────

    @Override
    public CommandHandler<WalletCommand, WalletEvent, WalletState> commandHandler() {
        /*var builder = newCommandHandlerBuilder();

        builder.forNullState()
                .onCommand(WalletCommand.CreateWallet.class, (walletState, createWallet) -> {
                    try {
                        var walletCreated2 = WalletAggregate.createWallet(createWallet);
                        return Effect().persist(walletCreated2)
                                .thenReply(createWallet.replyTo(), __ -> StatusReply.Ack());
                    } catch (Exception e) {
                        return Effect().none().thenReply(createWallet.replyTo(), __ -> StatusReply.error(""));
                    }
                })
                .onAnyCommand(() -> Effect().none().thenReply(null, __ -> StatusReply.error("Wallet Not Found")));

        builder.forNonNullState()
                .onCommand(WalletCommand.Deposit2.class, (walletAggregate, deposit2) -> {
                    walletAggregate.validate(deposit2)
                            .map(event -> {
                                Effect().persist(event)
                                        .thenReply(deposit2.replyTo(), StatusReply.Ack());
                            })
                            .recover(throwable -> Effect().none()
                                    .thenReply(deposit2.replyTo(), StatusReply.error(throwable.getMessage())))
                            .get()
                });

        return builder.build();*/
        return newCommandHandlerBuilder()
                .forAnyState()
                .onCommand(WalletCommand.CreateWallet.class, this::onCreateWallet)
                .onCommand(WalletCommand.Deposit.class, this::onDeposit)
                .onCommand(WalletCommand.Withdraw.class, this::onWithdraw)
                .onCommand(WalletCommand.Freeze.class, this::onFreeze)
                .onCommand(WalletCommand.Unfreeze.class, this::onUnfreeze)
                .onCommand(WalletCommand.Spend.class, this::onSpend)
                .onCommand(WalletCommand.FreezeForT0.class, this::onFreezeForT0)
                .onCommand(WalletCommand.SpendT0.class, this::onSpendT0)
                .onCommand(WalletCommand.AddCredit.class, this::onAddCredit)
                .onCommand(WalletCommand.GetWallet.class, this::onGetWallet)
                .onCommand(WalletCommand.GetBuyingPower.class, this::onGetBuyingPower)
                .onCommand(WalletCommand.ChargeSeparCredit.class, this::onChargeSeparCredit)
                .onCommand(WalletCommand.SettleSeparCredit.class, this::onSettleSeparCredit)
                .onCommand(WalletCommand.ReconcileFromRayan.class, this::onReconcile)
                .onCommand(WalletCommand.SeedFromLegacy.class, this::onSeed)
                .build();
    }

    private Effect<WalletEvent, WalletState> onCreateWallet(WalletState state, WalletCommand.CreateWallet cmd) {
        if (state.isCreated()) {
            return reject(cmd.replyTo(), 4002, "Wallet already exists for account " + accountNumber);
        }
        WalletState initial = new WalletState(
                cmd.walletId(), accountNumber,
                WalletState.Tier.ZERO, WalletState.Tier.ZERO, WalletState.Tier.ZERO,
                0L, 0L, 0L, 0L, WalletState.Debt.ZERO, List.of());
        return Effect().persist(new WalletEvent.WalletCreated(initial))
                .thenReply(cmd.replyTo(), WalletReply.Accepted::new);
    }

    private Effect<WalletEvent, WalletState> onDeposit(WalletState s, WalletCommand.Deposit c) {
        return onMutation(s, c.replyTo(), c.trackingId(),
                w -> w.deposit(c.trackingId(), new Money(c.amount()), c.settlementDelay(), c.type()));
    }

    private Effect<WalletEvent, WalletState> onWithdraw(WalletState s, WalletCommand.Withdraw c) {
        return onMutation(s, c.replyTo(), c.trackingId(),
                w -> w.withdraw(c.trackingId(), new Money(c.amount()), c.settlementDelay(), c.type()));
    }

    private Effect<WalletEvent, WalletState> onFreeze(WalletState s, WalletCommand.Freeze c) {
        return onMutation(s, c.replyTo(), c.trackingId(),
                w -> w.freeze(c.trackingId(), new Money(c.amount()), c.settlementDelay(), c.type(), c.canSpendSeparCredit()));
    }

    private Effect<WalletEvent, WalletState> onUnfreeze(WalletState s, WalletCommand.Unfreeze c) {
        return onMutation(s, c.replyTo(), c.trackingId(),
                w -> w.unfreeze(c.trackingId(), new Money(c.amount()), c.settlementDelay(), c.type()));
    }

    private Effect<WalletEvent, WalletState> onSpend(WalletState s, WalletCommand.Spend c) {
        return onMutation(s, c.replyTo(), c.trackingId(),
                w -> w.spend(c.trackingId(), new Money(c.amount()), c.settlementDelay(), c.type()));
    }

    private Effect<WalletEvent, WalletState> onFreezeForT0(WalletState s, WalletCommand.FreezeForT0 c) {
        return onMutation(s, c.replyTo(), c.trackingId(),
                w -> w.freeze(c.trackingId(), new Money(c.amount()), SettlementDelay.T_PLUS_0, c.type(), false));
    }

    private Effect<WalletEvent, WalletState> onSpendT0(WalletState s, WalletCommand.SpendT0 c) {
        return onMutation(s, c.replyTo(), c.trackingId(),
                w -> w.spend(c.trackingId(), new Money(c.amount()), SettlementDelay.T_PLUS_0, c.type()));
    }

    private Effect<WalletEvent, WalletState> onAddCredit(WalletState s, WalletCommand.AddCredit c) {
        return onMutation(s, c.replyTo(), c.trackingId(),
                w -> w.addCredit(c.trackingId(), new Money(c.creditAmount())));
    }

    /** Read: reply with the authoritative current state. No event persisted. */
    private Effect<WalletEvent, WalletState> onGetWallet(WalletState s, WalletCommand.GetWallet c) {
        if (!s.isCreated()) {
            return reject(c.replyTo(), ExceptionConstants.WALLET_NOT_EXIST.getCode(),
                    ExceptionConstants.WALLET_NOT_EXIST.getMessage());
        }
        return Effect().reply(c.replyTo(), new WalletReply.WalletSnapshot(s));
    }

    /** Read: compute buying power from the current state and reply. No event persisted. */
    private Effect<WalletEvent, WalletState> onGetBuyingPower(WalletState s, WalletCommand.GetBuyingPower c) {
        if (!s.isCreated()) {
            return reject(c.replyTo(), ExceptionConstants.WALLET_NOT_EXIST.getCode(),
                    ExceptionConstants.WALLET_NOT_EXIST.getMessage());
        }
        return Effect().reply(c.replyTo(),
                new WalletReply.BuyingPowerResult(s.toAggregate().buyingPower(c.settlementDelay())));
    }

    private Effect<WalletEvent, WalletState> onChargeSeparCredit(WalletState s, WalletCommand.ChargeSeparCredit c) {
        if (!s.isCreated()) {
            log.warn("ChargeSeparCredit for non-existent wallet {}", accountNumber);
            return Effect().none();
        }
        return onFireAndForget(s,
                w -> {
                    if (c.isLoan()) {
                        w.setSeparInitialCredit(c.amount());
                        w.setSeparCredit(c.amount());
                    } else {
                        w.decreaseSeparCredit(c.amount());
                    }
                }, null);
    }

    private Effect<WalletEvent, WalletState> onSettleSeparCredit(WalletState s, WalletCommand.SettleSeparCredit c) {
        if (!s.isCreated()) {
            log.warn("SettleSeparCredit for non-existent wallet {}", accountNumber);
            return Effect().none();
        }
        return onFireAndForget(s,
                w -> {
                    w.setSeparCredit(w.getSeparCredit() - w.getSeparInitialCredit());
                    w.setSeparInitialCredit(0L);
                }, null);
    }

    /** Fire-and-forget reconcile from the authoritative Rayan snapshot (target state from the caller). */
    private Effect<WalletEvent, WalletState> onReconcile(WalletState s, WalletCommand.ReconcileFromRayan c) {
        if (!s.isCreated()) {
            log.warn("ReconcileFromRayan for non-existent wallet {}", accountNumber);
            return Effect().none();
        }
        WalletState target = c.target().withSeenTrackingIds(s.seenTrackingIds());
        return Effect().persist(new WalletEvent.WalletMutated(List.of(), target));
    }

    /** One-off migration seed; idempotent (no-op if the entity already exists). */
    private Effect<WalletEvent, WalletState> onSeed(WalletState s, WalletCommand.SeedFromLegacy c) {
        if (s.isCreated()) {
            return Effect().none();
        }
        return Effect().persist(new WalletEvent.WalletSeeded(c.state()));
    }

    /**
     * Run a mutation that needs a reply, reusing the aggregate cascade. Validates idempotency,
     * applies the mutation on a transient {@link Wallet}, and persists a {@link WalletEvent.WalletMutated}.
     */
    private Effect<WalletEvent, WalletState> onMutation(WalletState state, ActorRef<WalletReply> replyTo,
                                                         UUID trackingId, Mutation mutation) {
        if (!state.isCreated()) {
            return reject(replyTo, ExceptionConstants.WALLET_NOT_EXIST.getCode(),
                    ExceptionConstants.WALLET_NOT_EXIST.getMessage());
        }
        if (trackingId != null && state.seenTrackingIds().contains(trackingId)) {
            return reject(replyTo, ExceptionConstants.DUPLICATE_TRACKING_ID.getCode(),
                    ExceptionConstants.DUPLICATE_TRACKING_ID.getMessage());
        }
        Wallet w = state.toAggregate();
        try {
            mutation.apply(w);
        } catch (ApplicationException | BusinessException e) {
            return reject(replyTo, codeOf(e), e.getMessage());
        }
        return commit(state, w, trackingId, replyTo);
    }

    /** Fire-and-forget variant: persists the mutation but does not reply. */
    private Effect<WalletEvent, WalletState> onFireAndForget(WalletState state, Mutation mutation, UUID trackingId) {
        Wallet w = state.toAggregate();
        try {
            mutation.apply(w);
        } catch (ApplicationException | BusinessException e) {
            log.warn("Fire-and-forget wallet mutation rejected for {}: {}", accountNumber, e.getMessage());
            return Effect().none();
        }
        List<WalletTransaction> legs = List.copyOf(w.getWalletTransactions());
        List<UUID> seen = appendTrimmed(state.seenTrackingIds(), trackingId);
        WalletState resulting = WalletState.fromAggregate(w, seen);
        return Effect().persist(new WalletEvent.WalletMutated(legs, resulting));
    }

    private Effect<WalletEvent, WalletState> commit(WalletState state, Wallet w,
                                                     UUID trackingId, ActorRef<WalletReply> replyTo) {
        List<WalletTransaction> legs = List.copyOf(w.getWalletTransactions());
        List<UUID> seen = appendTrimmed(state.seenTrackingIds(), trackingId);
        WalletState resulting = WalletState.fromAggregate(w, seen);
        return Effect().persist(new WalletEvent.WalletMutated(legs, resulting))
                .thenReply(replyTo, st -> new WalletReply.Accepted(st));
    }

    // ── event handling (idempotent: state = event's resulting state) ───────────

    @Override
    public EventHandler<WalletState, WalletEvent> eventHandler() {
        return newEventHandlerBuilder()
                .forAnyState()
                .onEvent(WalletEvent.WalletCreated.class, (state, evt) -> evt.initialState())
                .onEvent(WalletEvent.WalletMutated.class, (state, evt) -> evt.resultingState())
                .onEvent(WalletEvent.WalletSeeded.class, (state, evt) -> evt.state())
                .build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Effect<WalletEvent, WalletState> reject(ActorRef<WalletReply> replyTo, int code, String message) {
        return Effect().reply(replyTo, new WalletReply.Rejected(message == null ? "" : message, code));
    }

    private static int codeOf(Exception e) {
        if (e instanceof BusinessException be && be.getCode() != null) {
            return be.getCode();
        }
        // Domain (ApplicationException) rejections surface as code 4005 — matches the prior
        // WalletEvent.OperationFailed → BusinessException(message, 4005) facade mapping.
        return 4005;
    }

    private static List<UUID> appendTrimmed(List<UUID> seen, UUID trackingId) {
        if (trackingId == null) {
            return seen;
        }
        List<UUID> next = new ArrayList<>(seen.size() + 1);
        next.add(trackingId);
        next.addAll(seen);
        if (next.size() > SEEN_TRACKING_IDS) {
            next = new ArrayList<>(next.subList(0, SEEN_TRACKING_IDS));
        }
        return List.copyOf(next);
    }

    /** A wallet mutation that may throw a checked domain {@link ApplicationException}. */
    @FunctionalInterface
    private interface Mutation {
        void apply(Wallet w) throws ApplicationException;
    }
}
