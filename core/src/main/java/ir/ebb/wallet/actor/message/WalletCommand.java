package ir.ebb.wallet.actor.message;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import org.apache.pekko.actor.typed.ActorRef;

import java.util.UUID;

public sealed interface WalletCommand {

    record GetWallet(
            User user,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record Deposit(
            UUID trackingId,
            User user,
            long amount,
            SettlementDelay settlementDelay,
            WalletTransactionType type,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record Withdraw(
            UUID trackingId,
            User user,
            long amount,
            SettlementDelay settlementDelay,
            WalletTransactionType type,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record Freeze(
            UUID trackingId,
            User user,
            long amount,
            SettlementDelay settlementDelay,
            WalletTransactionType type,
            boolean canSpendSeparCredit,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record Unfreeze(
            UUID trackingId,
            User user,
            long amount,
            SettlementDelay settlementDelay,
            WalletTransactionType type,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record Spend(
            UUID trackingId,
            User user,
            long amount,
            SettlementDelay settlementDelay,
            WalletTransactionType type,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record FreezeForT0(
            UUID trackingId,
            User user,
            long amount,
            WalletTransactionType type,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record SpendT0(
            UUID trackingId,
            User user,
            long amount,
            WalletTransactionType type,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record AddCredit(
            UUID trackingId,
            User user,
            long creditAmount,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record ChargeSeparCredit(
            User user,
            long amount,
            boolean isLoan,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    record SettleSeparCredit(
            User user,
            ActorRef<WalletEvent> replyTo
    ) implements WalletCommand {}

    /** Internal passivation signal sent by the actor's own timer. */
    record Passivate() implements WalletCommand {}
}
