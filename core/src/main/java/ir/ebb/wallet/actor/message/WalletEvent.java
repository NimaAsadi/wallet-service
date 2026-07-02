package ir.ebb.wallet.actor.message;

import ir.ebb.wallet.aggregate.Wallet;

public sealed interface WalletEvent {

    record WalletUpdated(Wallet wallet) implements WalletEvent {}

    record OperationFailed(String message) implements WalletEvent {}
}
