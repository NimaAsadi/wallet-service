package ir.ebb.wallet.serialization;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.modules.ObjectWriterModule;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorRefResolver;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.serialization.SerializerWithStringManifest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Pekko serializer for every wallet protocol message (commands, events, replies, state) backed
 * by Alibaba Fastjson2. Bound once to the {@code ir.ebb.wallet.wallet.WalletSerializable} marker
 * in {@code application.conf}; replaces the previous Jackson JSON binding.
 *
 * <h3>Design</h3>
 * <ul>
 *   <li>Extends {@link SerializerWithStringManifest}: Pekko stores the string manifest returned
 *       by {@link #manifest(Object)} alongside the payload and feeds it back to
 *       {@link #fromBinary(byte[], String)}. Manifests are stable, explicit, versioned literals
 *       from {@link ManifestRegistry} — never Java class names.</li>
 *   <li>No reflection-based class lookup: {@link ManifestRegistry#classFor(String)} is a
 *       {@code Map.get}; the manifest is plain data.</li>
 *   <li>UTF-8 JSON via {@code JSON.toJSONBytes} / {@code JSON.parseObject}.</li>
 *   <li>Security-first Fastjson2 configuration: {@link JSONWriter.Feature#WriteClassName} is
 *       never enabled (no {@code @type} is ever emitted); the read context enables none of
 *       {@link JSONReader.Feature#SupportAutoType}, {@link JSONReader.Feature#SupportClassForName},
 *       or {@link JSONReader.Feature#FieldBased}, so no class is ever loaded from a payload.</li>
 *   <li>Enums serialize by {@code .name()} ({@link JSONWriter.Feature#WriteEnumsUsingName}).</li>
 *   <li>{@link ActorRef} (carried by ask commands, serialized for inter-node sharding delivery)
 *       round-trips via Pekko's {@link ActorRefResolver}, wired as a Fastjson2
 *       {@link ObjectWriter}/{@link ObjectReader} — exactly the mechanism Pekko's own Jackson
 *       module uses.</li>
 *   <li>Instance-scoped providers: no global/static Fastjson2 state is mutated.</li>
 *   <li>Schema evolution via an immutable {@link Migration} chain.</li>
 * </ul>
 *
 * <p>Thread-safe: the two {@code Context}s are effectively immutable after construction (features
 * in a primitive; providers backed by concurrent maps) and a fresh reader/writer is allocated per
 * call, so a single serializer instance is shared across the cluster.
 */
public final class FastJsonSerializer extends SerializerWithStringManifest {

    private final ManifestRegistry registry;
    private final Map<String, Migration> migrations;
    private final ActorRefResolver resolver; // null when no ActorSystem (pure unit-test mode)
    private final JSONWriter.Context writeContext;
    private final JSONReader.Context readContext;

    /** Pekko-injected constructor (the serialization extension passes the {@link ExtendedActorSystem}). */
    public FastJsonSerializer(ExtendedActorSystem system) {
        this(system, ManifestRegistry.WALLET, Map.of());
    }

    /**
     * Full constructor: custom registry + migration chain. Package-private — the production
     * binding uses {@link #FastJsonSerializer(ExtendedActorSystem)}; tests may pass a {@code null}
     * system (skipping {@link ActorRef} support) for pure JSON round-trips, or a real system for
     * the {@link ActorRef} path.
     */
    FastJsonSerializer(ExtendedActorSystem system, ManifestRegistry registry, Map<String, Migration> migrations) {
        this.registry = registry;
        this.migrations = Map.copyOf(migrations);
        this.resolver = (system == null) ? null : ActorRefResolver.get(Adapter.toTyped(system));

        // Instance-scoped providers keep this serializer's ActorRef adapters out of Fastjson2's
        // global defaults. Fresh providers still build readers/writers for any class (records,
        // POJOs, enums, UUID, ...) via the default ASM creators.
        ObjectWriterProvider writerProvider = new ObjectWriterProvider();
        ObjectReaderProvider readerProvider = new ObjectReaderProvider();
        if (this.resolver != null) {
            final ObjectWriter<ActorRef<?>> writer = new ActorRefWriter(this.resolver);
            final ObjectReader<ActorRef<?>> reader = new ActorRefReader(this.resolver);
            // Fastjson2 resolves a field value's ObjectWriter/ObjectReader by the value's RUNTIME
            // class (a concrete ActorRef impl), not the declared field type — so registering for
            // ActorRef.class alone misses it and Fastjson2 falls back to bean serialization of the
            // ref. A module is consulted for every type during (de)serialization; matching by
            // assignability covers every ActorRef subtype and parameterization
            // (ActorRef<WalletReply>, ActorRef<Done>, ...) in one place — no reflective walk over
            // the protocol records is needed.
            writerProvider.register(new ObjectWriterModule() {
                @Override
                public ObjectWriter<?> getObjectWriter(Type objectType, Class objectClass) {
                    return objectClass != null && ActorRef.class.isAssignableFrom(objectClass) ? writer : null;
                }
            });
            readerProvider.register(new ObjectReaderModule() {
                @Override
                public ObjectReader<?> getObjectReader(Type type) {
                    Class<?> raw = rawClass(type);
                    return raw != null && ActorRef.class.isAssignableFrom(raw) ? reader : null;
                }
            });
        }

        // Write: enums by name. WriteClassName is NOT enabled, so no @type/@class leaks out.
        this.writeContext = new JSONWriter.Context(writerProvider, JSONWriter.Feature.WriteEnumsUsingName);
        // Read: no features -> SupportAutoType / SupportClassForName / FieldBased all clear.
        this.readContext = new JSONReader.Context(readerProvider);
    }

    @Override
    public int identifier() {
        return SerializationIds.WALLET_FASTJSON;
    }

    @Override
    public String manifest(Object object) {
        return registry.manifestOf(object.getClass());
    }

    @Override
    public byte[] toBinary(Object object) {
        try {
            return JSON.toJSONBytes(object, StandardCharsets.UTF_8, writeContext);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize " + object.getClass().getName(), e);
        }
    }

    @Override
    public Object fromBinary(byte[] bytes, String manifest) {
        try {
            String current = manifest;
            String json = new String(bytes, StandardCharsets.UTF_8);

            // Walk the migration chain (v1 -> v2 -> ...) over the parsed JSON until a manifest
            // known to the registry is reached, then bind the final JSON to its class.
            Migration migration = migrations.get(current);
            if (migration != null) {
                JSONObject node = JSON.parseObject(json);
                while (migration != null) {
                    node = migration.migrate(node);
                    current = migration.toManifest();
                    migration = migrations.get(current);
                }
                json = node.toString();
            }

            Class<?> target = registry.classFor(current);
            return JSON.parseObject(json, (Type) target, readContext);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializationException(
                    "Corrupted or invalid JSON payload for manifest '" + manifest + "'", e);
        }
    }

    // ── ActorRef <-> serialization-format string (delegates to Pekko's ActorRefResolver) ─────

    /** Raw class of a (possibly parameterized) {@link Type}, or {@code null} if undetermined. */
    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> cls) {
            return cls;
        }
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> cls) {
            return cls;
        }
        return null;
    }

    /**
     * Writes an {@link ActorRef} as its canonical Pekko serialization-format path string — the
     * same form Pekko's own Jackson module emits — so the ref resolves on the receiving node.
     */
    private static final class ActorRefWriter implements ObjectWriter<ActorRef<?>> {

        private final ActorRefResolver resolver;

        ActorRefWriter(ActorRefResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public void write(JSONWriter writer, Object value, Object fieldName, Type fieldType, long features) {
            if (value == null) {
                writer.writeNull();
                return;
            }
            writer.writeString(resolver.toSerializationFormat((ActorRef<?>) value));
        }
    }

    /** Reads the Pekko serialization-format string back into a live {@link ActorRef}. */
    private static final class ActorRefReader implements ObjectReader<ActorRef<?>> {

        private final ActorRefResolver resolver;

        ActorRefReader(ActorRefResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public ActorRef<?> readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {
            if (reader.nextIfNull()) {
                return null;
            }
            String serialized = reader.readString();
            return serialized == null ? null : resolver.resolveActorRef(serialized);
        }
    }
}
