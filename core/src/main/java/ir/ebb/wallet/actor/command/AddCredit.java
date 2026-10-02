package ir.ebb.wallet.actor.command;

import ir.ebb.wallet.constant.valueobject.Money;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;

import java.util.UUID;

/**
 * Admin credit op: a DELTA on top of the current credit/initialCredit (not an absolute target).
 * A zero value is a legal audit-only op (the admin "remove credit" endpoint).
 */
public record AddCredit(
        UUID trackingId,
        Money value,
        ActorRef<StatusReply<Done>> replyTo
) implements WalletCommand {
}
