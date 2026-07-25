package ir.ebb.wallet.aggregate;

import io.vavr.control.Try;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.wallet.WalletCommand;
import ir.ebb.wallet.wallet.WalletEvent;
import ir.ebb.wallet.wallet.WalletSerializable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WalletAggregate implements WalletSerializable {

    private final Wallet wallet;
    private final Set<UUID> trackingIds;

    public WalletAggregate(Wallet wallet, Set<UUID> trackingIds) {
        this.wallet = wallet;
        this.trackingIds = trackingIds;
    }

    public static WalletEvent.WalletCreated2 createWallet(WalletCommand.CreateWallet command) {
        var wallet = new Wallet(null);
        return new WalletEvent.WalletCreated2(wallet);
    }

    public static WalletAggregate applyEvent(WalletEvent.WalletCreated2 event) {
        return new WalletAggregate(event.wallet(), new HashSet<>());
    }

    public Try<WalletEvent> validate(WalletCommand command) {
        return Try.of(() -> {
            if (command instanceof WalletCommand.Deposit2 d) {
                return new WalletEvent.WalletDeposited(d.amount(), d.settlementDelay(), d.trackingId());
            }
            throw new RuntimeException("");
        });
    }

    public WalletAggregate applyEvent(WalletEvent event) {
        if (event instanceof WalletEvent.WalletDeposited(
                Long amount, ir.ebb.common.constant.enumeration.SettlementDelay settlementDelay, UUID trackingId
        )) {
            wallet.deposit(trackingId, new Money(amount), settlementDelay, null);
            trackingIds.add(trackingId);
        }
        return this;
    }
}
