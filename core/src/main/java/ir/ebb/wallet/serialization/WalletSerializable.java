package ir.ebb.wallet.serialization;

/**
 * Marker interface for the next-gen wallet protocol types ({@code actor.command.WalletCommand}
 * commands, {@code actor.event.WalletEvent events}, {@code aggregate.WalletAggregate state}) that
 * are serialized by Pekko with the custom Fastjson2 serializer
 * {@code ir.ebb.wallet.serialization.FastJsonSerializer} (replies use Pekko's {@code StatusReply},
 * which is not serialized by this serializer).
 *
 * <p>Bound once in {@code application.conf}:
 * <pre>
 * pekko.actor {
 *   serializers { wallet-fastjson = "ir.ebb.wallet.serialization.FastJsonSerializer" }
 *   serialization-identifiers { wallet-fastjson = 700001 }
 *   serialization-bindings {
 *     "ir.ebb.wallet.serialization.WalletSerializable" = wallet-fastjson
 *   }
 * }
 * </pre>
 * The serializer uses explicit, versioned string manifests (e.g. {@code "actor-aggregate:v1"})
 * from an immutable manifest→class registry — never Java class names. {@code ActorRef}s inside
 * commands round-trip via Pekko's {@code ActorRefResolver}. See {@code ir.ebb.wallet.serialization}
 * for the full implementation.
 */
public interface WalletSerializable {
}
