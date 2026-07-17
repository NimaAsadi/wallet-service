package ir.ebb.wallet.wallet;

/**
 * Marker interface for the wallet protocol types ({@link WalletCommand commands},
 * {@link WalletEvent events}, {@link WalletReply replies}, {@link WalletState state})
 * that are serialized with Pekko Jackson <em>CBOR</em>.
 *
 * <p>Bound once in {@code application.conf}:
 * <pre>
 * pekko.actor.serialization-bindings {
 *   "ir.ebb.wallet.wallet.WalletSerializable" = jackson-cbor
 * }
 * </pre>
 * The concrete record type travels in the serialization manifest (Pekko's
 * {@code type-in-manifest = on} default), so sealed interfaces need no
 * {@code @JsonTypeInfo} annotations. {@code ActorRef}s inside commands are handled
 * by the auto-registered {@code PekkoTypedJacksonModule}.
 */
public interface WalletSerializable {
}
