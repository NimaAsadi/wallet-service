package ir.ebb.wallet.actor.command;

import ir.ebb.wallet.actor.WalletSnapshot;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;

/**
 * Read-only, strongly-consistent single-wallet read: served from the entity's in-memory state
 * ({@code Effect().none()} — nothing persisted). A wallet that does not exist is rejected with
 * {@code WALLET_NOT_EXIST} by the null-state handler.
 */
public record GetWallet(
        ActorRef<StatusReply<WalletSnapshot>> replyTo
) implements WalletCommand {
}
