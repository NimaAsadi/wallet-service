package ir.ebb.wallet.serialization;

import ir.ebb.wallet.actor.command.CreateWallet;
import ir.ebb.wallet.actor.command.Deposit;
import ir.ebb.wallet.actor.command.Freeze;
import ir.ebb.wallet.actor.command.Spend;
import ir.ebb.wallet.actor.command.Unfreeze;
import ir.ebb.wallet.actor.command.Withdraw;
import ir.ebb.wallet.actor.event.Deposited;
import ir.ebb.wallet.actor.event.Frozen;
import ir.ebb.wallet.actor.event.Spent;
import ir.ebb.wallet.actor.event.Unfrozen;
import ir.ebb.wallet.actor.event.WalletCreated;
import ir.ebb.wallet.actor.event.Withdrew;
import ir.ebb.wallet.aggregate.WalletAggregate;
import ir.ebb.wallet.wallet.WalletCommand;
import ir.ebb.wallet.wallet.WalletEvent;
import ir.ebb.wallet.wallet.WalletReply;
import ir.ebb.wallet.wallet.WalletState;

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
     * The wallet protocol registry: all 26 concrete {@code WalletSerializable} types
     * (commands, events, replies, state). Events/state/snapshots use the {@code wallet-*}
     * family (the persisted, most-read names); commands use {@code cmd-*}.
     */
    public static final ManifestRegistry WALLET = builder()
            // ── Events (persisted to the journal) ───────────────────────────────────────
            .register("wallet-created:v1", WalletEvent.WalletCreated.class)
            .register("wallet-mutated:v1", WalletEvent.WalletMutated.class)
            .register("wallet-seeded:v1", WalletEvent.WalletSeeded.class)
            // Sealed-permitted but presently unused by the event handler (defensive: keep the
            // full sealed surface serializable so a future revival never throws at the wire).
            .register("wallet-created2:v1", WalletEvent.WalletCreated2.class)
            .register("wallet-deposited:v1", WalletEvent.WalletDeposited.class)
            // ── State (event payloads + the persistence snapshot type) ──────────────────
            .register("wallet-state:v1", WalletState.class)
            // ── Replies (inter-node, never persisted) ───────────────────────────────────
            .register("wallet-snapshot:v1", WalletReply.WalletSnapshot.class)
            .register("wallet-accepted:v1", WalletReply.Accepted.class)
            .register("wallet-rejected:v1", WalletReply.Rejected.class)
            .register("wallet-buying-power-result:v1", WalletReply.BuyingPowerResult.class)
            // ── Commands (inter-node via cluster sharding) ──────────────────────────────
            .register("cmd-create-wallet:v1", WalletCommand.CreateWallet.class)
            .register("cmd-deposit:v1", WalletCommand.Deposit.class)
            .register("cmd-withdraw:v1", WalletCommand.Withdraw.class)
            .register("cmd-freeze:v1", WalletCommand.Freeze.class)
            .register("cmd-unfreeze:v1", WalletCommand.Unfreeze.class)
            .register("cmd-spend:v1", WalletCommand.Spend.class)
            .register("cmd-freeze-for-t0:v1", WalletCommand.FreezeForT0.class)
            .register("cmd-spend-t0:v1", WalletCommand.SpendT0.class)
            .register("cmd-add-credit:v1", WalletCommand.AddCredit.class)
            .register("cmd-get-wallet:v1", WalletCommand.GetWallet.class)
            .register("cmd-get-buying-power:v1", WalletCommand.GetBuyingPower.class)
            .register("cmd-charge-separ-credit:v1", WalletCommand.ChargeSeparCredit.class)
            .register("cmd-settle-separ-credit:v1", WalletCommand.SettleSeparCredit.class)
            .register("cmd-reconcile-from-rayan:v1", WalletCommand.ReconcileFromRayan.class)
            .register("cmd-seed-from-legacy:v1", WalletCommand.SeedFromLegacy.class)
            // Sealed-permitted reply-to variant (ActorRef<Done>); currently unused by the command
            // handler — registered defensively so the full sealed command surface serializes.
            .register("cmd-deposit2:v1", WalletCommand.Deposit2.class)
            // ── Next-gen actor events (ir.ebb.wallet.actor.event; journal-persisted) ──────────
            .register("actor-wallet-created:v1", WalletCreated.class)
            .register("actor-deposited:v1", Deposited.class)
            .register("actor-frozen:v1", Frozen.class)
            .register("actor-unfrozen:v1", Unfrozen.class)
            .register("actor-spent:v1", Spent.class)
            .register("actor-withdrew:v1", Withdrew.class)
            // ── Next-gen actor state (persistence state / embedded in WalletCreated) ─────────
            // WalletAggregate round-trips via its no-arg constructor + setters (see its javadoc).
            .register("actor-wallet-aggregate:v1", WalletAggregate.class)
            // ── Next-gen actor commands (ir.ebb.wallet.actor.command; inter-node ask) ────────
            .register("actor-cmd-create-wallet:v1", CreateWallet.class)
            .register("actor-cmd-deposit:v1", Deposit.class)
            .register("actor-cmd-withdraw:v1", Withdraw.class)
            .register("actor-cmd-freeze:v1", Freeze.class)
            .register("actor-cmd-unfreeze:v1", Unfreeze.class)
            .register("actor-cmd-spend:v1", Spend.class)
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
