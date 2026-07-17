package ir.ebb.wallet.wallet;

/**
 * Reply protocol for {@link ir.ebb.wallet.wallet.command.WalletCommand}s that carry a
 * reply {@link org.apache.pekko.actor.typed.ActorRef}. {@code Accepted} returns the
 * resulting state; {@code Rejected} carries the domain message + error code (from
 * {@link ir.ebb.base.exception.ExceptionConstants}) — the facade maps a rejection to a
 * {@link ir.ebb.common.exception.handler.BusinessException} for the HTTP/gRPC layer.
 */
public sealed interface WalletReply extends WalletSerializable {

    record Accepted(WalletState state) implements WalletReply {}

    record Rejected(String message, int code) implements WalletReply {}
}
