package ir.ebb.wallet.actor.event;

import ir.ebb.wallet.aggregate.WalletAggregate;

public record WalletCreated(WalletAggregate wallet) implements WalletEvent {
}
