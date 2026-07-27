package ir.ebb.wallet.actor.command;

import ir.ebb.wallet.wallet.WalletSerializable;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;

public interface WalletCommand extends WalletSerializable {
    <T> ActorRef<StatusReply<T>> replyTo();
}
