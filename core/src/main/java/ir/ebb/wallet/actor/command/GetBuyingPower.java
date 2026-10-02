package ir.ebb.wallet.actor.command;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;

/**
 * Read-only buying-power read for one settlement delay, computed inside the entity from its
 * in-memory state. A wallet that does not exist is rejected with {@code WALLET_NOT_EXIST}.
 */
public record GetBuyingPower(
        SettlementDelay settlementDelay,
        ActorRef<StatusReply<BuyingPower>> replyTo
) implements WalletCommand {
}
