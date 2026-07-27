package ir.ebb.wallet.actor.command;

import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;

public record CreateWallet(
        Long dbsAccountNumber,
        ActorRef<StatusReply<Done>> replyTo
) implements WalletCommand {
}
