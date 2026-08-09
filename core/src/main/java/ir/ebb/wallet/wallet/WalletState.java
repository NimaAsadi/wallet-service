package ir.ebb.wallet.wallet;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.aggregate.WalletDebt;
import ir.ebb.wallet.constant.valueobject.WalletParameter;

import java.util.List;
import java.util.UUID;

/**
 * Immutable snapshot of a wallet aggregate — the persisted state of the
 * {@code WalletEntity} event-sourced behavior (persisted as a snapshot, also JSON via
 * {@link WalletSerializable}) and the payload of each {@link WalletEvent.WalletMutated}
 * event. Because the wallet's money-movement cascade (see {@link Wallet}) is intricate
 * and already correct, the entity reuses that mutable aggregate as a transient
 * computation scratchpad: a command handler rebuilds a {@link Wallet} via
 * {@link #toAggregate()}, runs the mutation, then captures the result back into an
 * immutable {@code WalletState}.
 *
 * <p>Events carry the full resulting state (absolute values), which makes event
 * application idempotent: re-applying an event sets each field to the same absolute
 * value, a no-op. Per-leg audit detail travels in the event's {@code legs}.
 */
public record WalletState(
        UUID id,
        long accountNumber,
        Tier t0,
        Tier t1,
        Tier t2,
        long credit,
        long initialCredit,
        long separCredit,
        long separInitialCredit,
        Debt debt,
        List<UUID> seenTrackingIds
) implements WalletSerializable {

    /** One settlement tier's balance + frozen amounts. */
    public record Tier(long balance, long frozen) {
        public static final Tier ZERO = new Tier(0L, 0L);
    }

    /** The seven inter-tier / inter-credit IOU counters of {@link WalletDebt}. */
    public record Debt(
            long t2Tot0, long t2Tot1, long t1Tot0,
            long t2ToCredit, long t1ToCredit,
            long t2ToSeparCredit, long t1ToSeparCredit
    ) {
        public static final Debt ZERO = new Debt(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    /** State for an entity that has not yet been created (no {@code WalletCreated} / {@code SeedFromLegacy} event). */
    public static WalletState empty() {
        return new WalletState(null, 0L,  Tier.ZERO, Tier.ZERO, Tier.ZERO,
                0L, 0L, 0L, 0L, Debt.ZERO, List.of());
    }

    public boolean isCreated() {
        return id != null;
    }

    /** Rebuild the mutable {@link Wallet} aggregate from this state (transient scratchpad for command handling). */
    public Wallet toAggregate() {
        Wallet w = new Wallet(accountNumber);
        w.setId(id);
        w.setT0(new WalletParameter(t0.balance(), t0.frozen()));
        w.setT1(new WalletParameter(t1.balance(), t1.frozen()));
        w.setT2(new WalletParameter(t2.balance(), t2.frozen()));
        w.setCredit(credit);
        w.setInitialCredit(initialCredit);
        w.setSeparCredit(separCredit);
        w.setSeparInitialCredit(separInitialCredit);
        WalletDebt d = new WalletDebt();
        d.setT2Tot0Debt(debt.t2Tot0());
        d.setT2Tot1Debt(debt.t2Tot1());
        d.setT1Tot0Debt(debt.t1Tot0());
        d.setT2ToCreditDebt(debt.t2ToCredit());
        d.setT1ToCreditDebt(debt.t1ToCredit());
        d.setT2ToSeparCreditDebt(debt.t2ToSeparCredit());
        d.setT1ToSeparCreditDebt(debt.t1ToSeparCredit());
        w.setWalletDebt(d);
        return w;
    }

    /** Capture a mutated {@link Wallet} back into an immutable state. */
    public static WalletState fromAggregate(Wallet w, List<UUID> seenTrackingIds) {
        return new WalletState(
                w.getId(),
                w.getAccountNumber(),
                new Tier(w.getT0().getBalance(), w.getT0().getFrozen()),
                new Tier(w.getT1().getBalance(), w.getT1().getFrozen()),
                new Tier(w.getT2().getBalance(), w.getT2().getFrozen()),
                w.getCredit(), w.getInitialCredit(), w.getSeparCredit(), w.getSeparInitialCredit(),
                new Debt(
                        w.getWalletDebt().getT2Tot0Debt(),
                        w.getWalletDebt().getT2Tot1Debt(),
                        w.getWalletDebt().getT1Tot0Debt(),
                        w.getWalletDebt().getT2ToCreditDebt(),
                        w.getWalletDebt().getT1ToCreditDebt(),
                        w.getWalletDebt().getT2ToSeparCreditDebt(),
                        w.getWalletDebt().getT1ToSeparCreditDebt()),
                seenTrackingIds
        );
    }

    /** Copy with a replacement seen-trackingId list (e.g. when reconciling from Rayan). */
    public WalletState withSeenTrackingIds(List<UUID> seen) {
        return new WalletState(id, accountNumber,  t0, t1, t2,
                credit, initialCredit, separCredit, separInitialCredit, debt, seen);
    }
}
