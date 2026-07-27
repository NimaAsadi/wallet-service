package ir.ebb.wallet.wallet;

/**
 * Marker interface for the wallet protocol types ({@link WalletCommand commands},
 * {@link WalletEvent events}, {@link WalletReply replies}, {@link WalletState state})
 * that are serialized by Pekko with the custom Fastjson2 serializer
 * {@code ir.ebb.wallet.serialization.FastJsonSerializer}.
 *
 * <p>Bound once in {@code application.conf}:
 * <pre>
 * pekko.actor {
 *   serializers { wallet-fastjson = "ir.ebb.wallet.serialization.FastJsonSerializer" }
 *   serialization-identifiers { wallet-fastjson = 700001 }
 *   serialization-bindings {
 *     "ir.ebb.wallet.wallet.WalletSerializable" = wallet-fastjson
 *   }
 * }
 * </pre>
 * The serializer uses explicit, versioned string manifests (e.g. {@code "wallet-state:v1"})
 * from an immutable manifest→class registry — never Java class names. {@code ActorRef}s
 * inside commands round-trip via Pekko's {@code ActorRefResolver}. See
 * {@code ir.ebb.wallet.serialization} for the full implementation.
 */
public interface WalletSerializable {
}
