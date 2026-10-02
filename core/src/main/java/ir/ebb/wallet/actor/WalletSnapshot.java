package ir.ebb.wallet.actor;

import ir.ebb.wallet.aggregate.WalletAggregate;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import ir.ebb.wallet.serialization.WalletSerializable;
import ir.ebb.wallet.valueobject.Wallet;

/**
 * Immutable point-in-time copy of the {@link WalletAggregate}, the reply payload of the
 * {@code GetWallet} read command. Never hand out the live aggregate — the entity keeps mutating
 * it (and its {@link WalletParameter} tiers) in place, so a shared reference would be mutable
 * state leaking across threads; {@code from} deep-copies the tiers.
 */
public record WalletSnapshot(
        WalletParameter t0,
        WalletParameter t1,
        WalletParameter t2,
        Long credit,
        Long initialCredit,
        Long separCredit,
        Long separInitialCredit,
        Long dbsAccountNumber
) implements WalletSerializable {

    public static WalletSnapshot from(WalletAggregate aggregate) {
        return new WalletSnapshot(
                copy(aggregate.getT0()),
                copy(aggregate.getT1()),
                copy(aggregate.getT2()),
                aggregate.getCredit(),
                aggregate.getInitialCredit(),
                aggregate.getSeparCredit(),
                aggregate.getSeparInitialCredit(),
                aggregate.getDbsAccountNumber()
        );
    }

    /**
     * Domain-view for the response transformers (user/bridge wallet DTOs adapt the
     * {@code valueobject.Wallet} shape). Debt/transaction legs are not part of a snapshot.
     */
    public Wallet toWallet() {
        Wallet wallet = new Wallet(dbsAccountNumber == null ? 0L : dbsAccountNumber);
        wallet.setT0(copy(t0));
        wallet.setT1(copy(t1));
        wallet.setT2(copy(t2));
        wallet.setCredit(credit);
        wallet.setInitialCredit(initialCredit);
        wallet.setSeparCredit(separCredit);
        wallet.setSeparInitialCredit(separInitialCredit);
        return wallet;
    }

    private static WalletParameter copy(WalletParameter p) {
        return p == null ? null : new WalletParameter(p.getBalance(), p.getFrozen());
    }
}
