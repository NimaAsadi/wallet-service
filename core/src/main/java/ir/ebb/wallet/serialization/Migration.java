package ir.ebb.wallet.serialization;

import com.alibaba.fastjson2.JSONObject;

/**
 * One step of a manifest-version migration: transforms the parsed old-shape JSON of
 * {@link #fromManifest()} into the new-shape JSON of {@link #toManifest()}.
 *
 * <p>Migrations are registered on {@link FastJsonSerializer} keyed by {@code fromManifest}.
 * On deserialization the serializer walks the chain ({@code v1 → v2 → …}) until it reaches a
 * manifest present in the {@link ManifestRegistry}, then binds the final JSON to the current
 * class. This keeps historical journal data readable forever after a type evolves, without ever
 * touching the stored bytes — only the parsed {@link JSONObject} is rewritten in memory.
 *
 * <p>Implementations must be pure (no I/O, no shared mutable state) and tolerant of partially-
 * migrated input (use {@link JSONObject#containsKey} guards before reading/renaming a field).
 *
 * <p>Example — adding a field with a default when {@code WalletState} moves {@code v1 → v2}:
 * <pre>{@code
 * final class WalletStateV1ToV2 implements Migration {
 *     public String fromManifest() { return "wallet-state:v1"; }
 *     public String toManifest()   { return "wallet-state:v2"; }
 *     public JSONObject migrate(JSONObject old) {
 *         if (!old.containsKey("frozenCredit")) old.put("frozenCredit", 0L);
 *         return old;
 *     }
 * }
 * }</pre>
 */
public interface Migration {

    /** Manifest the migration reads from (e.g. {@code "wallet-state:v1"}). */
    String fromManifest();

    /** Manifest the migration produces (e.g. {@code "wallet-state:v2"}). */
    String toManifest();

    /** Transform the parsed old-shape JSON in place and return it (may be the same object). */
    JSONObject migrate(JSONObject old);
}
