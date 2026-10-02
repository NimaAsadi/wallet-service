package ir.ebb.wallet.serialization;

import ir.ebb.wallet.actor.WalletSnapshot;
import ir.ebb.wallet.actor.command.AddCredit;
import ir.ebb.wallet.actor.command.CreateWallet;
import ir.ebb.wallet.actor.command.Deposit;
import ir.ebb.wallet.actor.command.Freeze;
import ir.ebb.wallet.actor.command.GetBuyingPower;
import ir.ebb.wallet.actor.command.GetWallet;
import ir.ebb.wallet.actor.command.Spend;
import ir.ebb.wallet.actor.command.Unfreeze;
import ir.ebb.wallet.actor.command.Withdraw;
import ir.ebb.wallet.actor.event.*;
import ir.ebb.wallet.aggregate.WalletAggregate;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, bidirectional registry of serialization manifests: {@code manifest → concrete class}
 * and its reverse {@code class → manifest}. There is <strong>no reflection</strong> for class
 * resolution — a lookup is a plain {@link Map#get}; classes are wired explicitly at construction
 * time and never derived from the payload (the manifest is plain data, never a Java class name).
 *
 * <p>Manifests are stable forever: they are string literals keyed here, so renaming a record
 * class does not change its manifest. Bumping a type to a new schema version adds a new
 * {@code :vN} entry (and a {@link Migration}); the old entry stays so historical data remains
 * readable.
 *
 * <p>Unknown manifests and unregistered classes throw a descriptive {@link SerializationException}
 * rather than silently degrading.
 */
public final class ManifestRegistry {

    private final Map<String, Class<?>> manifestToClass;
    private final Map<Class<?>, String> classToManifest;

    private ManifestRegistry(Map<String, Class<?>> manifestToClass, Map<Class<?>, String> classToManifest) {
        this.manifestToClass = manifestToClass;
        this.classToManifest = classToManifest;
    }

    /** Resolve the concrete class for a manifest, or throw if it is unknown. */
    public Class<?> classFor(String manifest) {
        Class<?> type = manifestToClass.get(manifest);
        if (type == null) {
            throw new SerializationException(
                    "Unknown manifest: '" + manifest + "' — not registered and no migration chain applies");
        }
        return type;
    }

    /** Resolve the manifest for a concrete class, or throw if it is not registered. */
    public String manifestOf(Class<?> type) {
        String manifest = classToManifest.get(type);
        if (manifest == null) {
            throw new SerializationException("Class not registered for serialization: " + type.getName());
        }
        return manifest;
    }

    /** Whether a manifest is known to this registry. */
    public boolean knows(String manifest) {
        return manifestToClass.containsKey(manifest);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The wallet protocol registry: every concrete {@code WalletSerializable} type — both the
     * legacy {@code wallet.*} protocol (commands, events, replies, state; {@code wallet-*} and
     * {@code cmd-*} families) and the next-gen {@code actor.*} protocol (commands, events and the
     * {@code WalletAggregate} state; {@code actor-*} family). One serializer instance (id 700001)
     * serves both, so one shared registry is required.
     */
    public static final ManifestRegistry WALLET = builder()
            // Sealed-permitted reply-to variant (ActorRef<Done>); currently unused by the command
            // handler — registered defensively so the full sealed command surface serializes.
            // ── Next-gen actor protocol (ir.ebb.wallet.actor.*/aggregate) ────────────────
            // Same serializer instance (id 700001); a distinct "actor-" manifest family keeps
            // these journal rows instantly distinguishable from legacy "wallet-"/"cmd-" rows.
            // ── Events (persisted to the journal) ────────────────────────────────────────
            .register("actor-created:v1", WalletCreated.class)
            .register("actor-deposited:v1", Deposited.class)
            .register("actor-frozen:v1", Frozen.class)
            .register("actor-unfrozen:v1", Unfrozen.class)
            .register("actor-spent:v1", Spent.class)
            .register("actor-withdrew:v1", Withdrew.class)
            .register("actor-credit-added:v1", CreditAdded.class)
            // ── State (event payload + the persistence snapshot type) ───────────────────
            .register("actor-aggregate:v1", WalletAggregate.class)
            // ── Commands (inter-node via cluster sharding) ──────────────────────────────
            .register("actor-cmd-create-wallet:v1", CreateWallet.class)
            .register("actor-cmd-deposit:v1", Deposit.class)
            .register("actor-cmd-withdraw:v1", Withdraw.class)
            .register("actor-cmd-freeze:v1", Freeze.class)
            .register("actor-cmd-unfreeze:v1", Unfreeze.class)
            .register("actor-cmd-spend:v1", Spend.class)
            .register("actor-cmd-add-credit:v1", AddCredit.class)
            .register("actor-cmd-get-wallet:v1", GetWallet.class)
            .register("actor-cmd-get-buying-power:v1", GetBuyingPower.class)
            // ── Read replies (ask responses travel the wire too) ────────────────────────
            .register("actor-snapshot:v1", WalletSnapshot.class)
            .register("actor-buying-power:v1", BuyingPower.class)
            .build();

    /**
     * Collects {@code (manifest, class)} pairs into an immutable {@link ManifestRegistry}.
     * Fails fast on duplicate manifests or duplicate classes (a programming error).
     */
    public static final class Builder {

        private final Map<String, Class<?>> manifestToClass = new LinkedHashMap<>();
        private final Map<Class<?>, String> classToManifest = new LinkedHashMap<>();

        public Builder register(String manifest, Class<?> type) {
            Objects.requireNonNull(manifest, "manifest");
            Objects.requireNonNull(type, "type");
            if (manifest.isBlank()) {
                throw new IllegalArgumentException("manifest must not be blank");
            }
            if (manifestToClass.putIfAbsent(manifest, type) != null) {
                throw new IllegalArgumentException("Duplicate manifest registration: " + manifest);
            }
            if (classToManifest.putIfAbsent(type, manifest) != null) {
                throw new IllegalArgumentException("Duplicate class registration: " + type.getName());
            }
            return this;
        }

        public ManifestRegistry build() {
            return new ManifestRegistry(Map.copyOf(manifestToClass), Map.copyOf(classToManifest));
        }
    }
}
