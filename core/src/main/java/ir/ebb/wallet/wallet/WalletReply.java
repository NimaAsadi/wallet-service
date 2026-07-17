package ir.ebb.wallet.wallet;

import ir.ebb.wallet.constant.valueobject.BuyingPower;

/**
 * Reply protocol for {@link WalletCommand}s that carry a
 * reply {@link org.apache.pekko.actor.typed.ActorRef}. {@code Accepted} returns the
 * resulting state; {@code Rejected} carries the domain message + error code (from
 * {@link ir.ebb.base.exception.ExceptionConstants}) — the facade maps a rejection to a
 * {@link ir.ebb.common.exception.handler.BusinessException} for the HTTP/gRPC layer.
 *
 * <p>Reads use {@link WalletSnapshot} / {@link BuyingPowerResult} (kept distinct from
 * {@code Accepted} so a read does not masquerade as an accepted mutation); a missing
 * wallet is reported via the shared {@code Rejected} (code {@code WALLET_NOT_EXIST}).
 */
public sealed interface WalletReply extends WalletSerializable {

    record Accepted(WalletState state) implements WalletReply {}

    record Rejected(String message, int code) implements WalletReply {}

    /** Read reply: the authoritative current state snapshot (no event persisted). */
    record WalletSnapshot(WalletState state) implements WalletReply {}

    /** Read reply: buying power computed inside the entity from its current state. */
    record BuyingPowerResult(BuyingPower buyingPower) implements WalletReply {}
}
