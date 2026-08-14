package ir.ebb.wallet.actor.command;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.Money;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;

import java.util.UUID;

public record Deposit(
        UUID trackingId,
        Money value,
        SettlementDelay settlementDelay,
        WalletTransactionType walletTransactionType,
        ActorRef<StatusReply<Done>> replyTo
) implements WalletCommand {
}
