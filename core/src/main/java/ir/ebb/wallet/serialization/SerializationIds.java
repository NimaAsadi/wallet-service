package ir.ebb.wallet.serialization;

/**
 * Stable, hand-assigned constants for the wallet Fastjson2 serializer.
 *
 * <ul>
 *   <li>{@link #WALLET_FASTJSON} — the Pekko serializer {@code identifier()} and the matching
 *       {@code pekko.actor.serialization-identifiers} value. Pekko stores this id in every
 *       serialized envelope / journal row and uses it (not the manifest) to pick the
 *       deserializer, so it must be globally unique across the whole cluster and never change.
 *       It is intentionally far outside Pekko/Jackson's low single/double-digit id range.</li>
 *   <li>{@link #V1} — the manifest version suffix baked into every registered manifest
 *       (e.g. {@code "wallet-state:v1"}). Bumping a type to a new version adds a {@code :v2}
 *       manifest plus a {@link Migration}; the old {@code :v1} manifest stays registered so
 *       historical data stays readable.</li>
 * </ul>
 */
public final class SerializationIds {

    /** Pekko serializer identifier for {@link FastJsonSerializer}. Never reuse, never change. */
    public static final int WALLET_FASTJSON = 700001;

    /** Current manifest schema version suffix. */
    public static final String V1 = "v1";

    private SerializationIds() {
    }
}
