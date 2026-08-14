package ir.ebb.wallet.serialization;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.valueobject.WalletTransaction;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.wallet.WalletCommand;
import ir.ebb.wallet.wallet.WalletEvent;
import ir.ebb.wallet.wallet.WalletReply;
import ir.ebb.wallet.wallet.WalletSerializable;
import ir.ebb.wallet.wallet.WalletState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link FastJsonSerializer} — no {@code ActorSystem}, so the
 * {@code ActorRef} path is skipped (constructed with a {@code null} system). Covers
 * round-trips for every non-{@code ActorRef} message kind, manifest stability, schema
 * evolution (migration / missing / additional fields), and error handling. The
 * {@code ActorRef} path is exercised by {@link FastJsonSerializerIntegrationTest}.
 */
class FastJsonSerializerTest {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final FastJsonSerializer serializer = new FastJsonSerializer(null);

    /**
     * Test-only value type exercising Fastjson2's default codecs for fields NOT yet present in the
     * wallet protocol ({@link BigDecimal}, {@link Instant}, {@link LocalDateTime}). Registered in a
     * local {@link ManifestRegistry} below — never shipped in production code.
     */
    record TemporalFixture(
            BigDecimal amount,
            Instant at,
            LocalDateTime local,
            UUID id,
            List<BigDecimal> amounts
    ) implements WalletSerializable {
    }

    // ── fixtures ───────────────────────────────────────────────────────────────────

    private WalletState sampleState() {
        return new WalletState(
                UUID.randomUUID(), 999L,
                User.of(UUID.randomUUID(), 999L),
                new WalletState.Tier(100L, 20L),
                new WalletState.Tier(50L, 0L),
                new WalletState.Tier(0L, 0L),
                200L, 200L, 30L, 30L,
                new WalletState.Debt(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                List.of(UUID.randomUUID(), UUID.randomUUID()));
    }

    private WalletTransaction leg() {
        return WalletTransaction.builder()
                .user(User.of(UUID.randomUUID(), 999L))
                .walletId(UUID.randomUUID())
                .walletOperationType(WalletOperationType.DEPOSIT)
                .walletTransactionType(WalletTransactionType.BANK_GATEWAY)
                .walletParameterType(WalletParameterType.T0)
                .amount(100L)
                .trackingId(UUID.randomUUID())
                .frozenBefore(0L).frozenAfter(0L)
                .balanceBefore(0L).balanceAfter(100L)
                .build();
    }

    private void assertRoundTrip(Object original) {
        String manifest = serializer.manifest(original);
        byte[] bytes = serializer.toBinary(original);
        Object roundTripped = serializer.fromBinary(bytes, manifest);
        assertThat(roundTripped).isEqualTo(original);
    }

    // ── serialize + deserialize ────────────────────────────────────────────────────

    @Test
    void roundTrips_events() {
        assertRoundTrip(new WalletEvent.WalletCreated(sampleState()));
        assertRoundTrip(new WalletEvent.WalletMutated(List.of(leg(), leg()), sampleState()));
        assertRoundTrip(new WalletEvent.WalletSeeded(sampleState()));
    }

    @Test
    void roundTrips_walletState() {
        assertRoundTrip(sampleState());
    }

    @Test
    void roundTrips_replies() {
        assertRoundTrip(new WalletReply.Accepted(sampleState()));
        assertRoundTrip(new WalletReply.Rejected("no funds", 4005));
        assertRoundTrip(new WalletReply.WalletSnapshot(sampleState()));
        assertRoundTrip(new WalletReply.BuyingPowerResult(new BuyingPower(100L, 200L, 30L)));
    }

    @Test
    void roundTrips_fireAndForgetCommands() {
        assertRoundTrip(new WalletCommand.ChargeSeparCredit(999L, 50L, true));
        assertRoundTrip(new WalletCommand.SettleSeparCredit(999L));
        assertRoundTrip(new WalletCommand.ReconcileFromRayan(sampleState()));
        assertRoundTrip(new WalletCommand.SeedFromLegacy(sampleState()));
    }

    // ── manifest stability (no class names; versioned; stable forever) ────────────

    @Test
    void manifest_returnsStableVersionedLiterals() {
        assertThat(serializer.manifest(new WalletEvent.WalletCreated(sampleState()))).isEqualTo("wallet-created:v1");
        assertThat(serializer.manifest(new WalletEvent.WalletMutated(List.of(), sampleState()))).isEqualTo("wallet-mutated:v1");
        assertThat(serializer.manifest(new WalletEvent.WalletSeeded(sampleState()))).isEqualTo("wallet-seeded:v1");
        assertThat(serializer.manifest(sampleState())).isEqualTo("wallet-state:v1");
        assertThat(serializer.manifest(new WalletReply.WalletSnapshot(sampleState()))).isEqualTo("wallet-snapshot:v1");
        assertThat(serializer.manifest(new WalletReply.Rejected("x", 1))).isEqualTo("wallet-rejected:v1");
        // an ActorRef command's manifest is a registry lookup — needs no serialization to resolve
        assertThat(serializer.manifest(new WalletCommand.ChargeSeparCredit(1L, 1L, false)))
                .isEqualTo("cmd-charge-separ-credit:v1");
        // no manifest must ever contain a Java class name
        String stateJson = new String(serializer.toBinary(sampleState()), UTF_8);
        assertThat(stateJson).doesNotContain("@type").doesNotContain("ir.ebb.");
    }

    // ── schema evolution ───────────────────────────────────────────────────────────

    @Test
    void versionMigration_transformsOldShapeBeforeBinding() {
        // Registry where the CURRENT schema is v2; v1 is reachable only via migration.
        ManifestRegistry regV2 = ManifestRegistry.builder()
                .register("wallet-state:v2", WalletState.class)
                .build();
        // Build v1-shape bytes: a real WalletState serialized, then "accountNumber" renamed to
        // a legacy key "acct" — simulating an old persisted schema.
        WalletState state = sampleState();
        FastJsonSerializer v2Writer = new FastJsonSerializer(null, regV2, Map.of());
        JSONObject node = JSON.parseObject(new String(v2Writer.toBinary(state), UTF_8));
        node.put("acct", node.get("accountNumber"));
        node.remove("accountNumber");
        byte[] v1Bytes = node.toString().getBytes(UTF_8);

        // Migration v1 -> v2 restores the current field name.
        Migration rename = new Migration() {
            @Override public String fromManifest() { return "wallet-state:v1"; }
            @Override public String toManifest() { return "wallet-state:v2"; }
            @Override public JSONObject migrate(JSONObject old) {
                if (old.containsKey("acct")) {
                    old.put("accountNumber", old.get("acct"));
                    old.remove("acct");
                }
                return old;
            }
        };
        FastJsonSerializer migratable = new FastJsonSerializer(null, regV2, Map.of(rename.fromManifest(), rename));

        WalletState recovered = (WalletState) migratable.fromBinary(v1Bytes, "wallet-state:v1");
        assertThat(recovered.accountNumber()).isEqualTo(999L);

        // Without the migration, v1 is an unknown manifest (not in the registry) and is rejected.
        assertThatThrownBy(() -> v2Writer.fromBinary(v1Bytes, "wallet-state:v1"))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Unknown manifest");
    }

    @Test
    void missingFields_defaultRatherThanFail() {
        // Forward/backward compatibility: a payload missing a primitive field yields its Java
        // default (0), a missing object field yields null — deserialization does not fail.
        byte[] missingCode = "{\"message\":\"oops\"}".getBytes(UTF_8);
        WalletReply.Rejected noCode = (WalletReply.Rejected) serializer.fromBinary(missingCode, "wallet-rejected:v1");
        assertThat(noCode.message()).isEqualTo("oops");
        assertThat(noCode.code()).isZero();
    }

    @Test
    void additionalFields_areIgnored() {
        WalletState state = sampleState();
        JSONObject node = JSON.parseObject(new String(serializer.toBinary(state), UTF_8));
        node.put("futureField", 42); // a field added by a newer writer
        byte[] bytes = node.toString().getBytes(UTF_8);

        WalletState recovered = (WalletState) serializer.fromBinary(bytes, "wallet-state:v1");
        assertThat(recovered).isEqualTo(state); // extra field tolerated, current type unchanged
    }

    // ── error handling ─────────────────────────────────────────────────────────────

    @Test
    void unknownManifest_throwsDescriptiveException() {
        assertThatThrownBy(() -> serializer.fromBinary("{}".getBytes(UTF_8), "no-such:v1"))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Unknown manifest")
                .hasMessageContaining("no-such:v1");
    }

    @Test
    void invalidJson_throwsSerializationException() {
        assertThatThrownBy(() -> serializer.fromBinary("{not json".getBytes(UTF_8), "wallet-state:v1"))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Corrupted or invalid");
    }

    @Test
    void corruptedPayload_throwsSerializationException() {
        // 0x8f is not a valid UTF-8 leading byte; decoding then parsing must fail cleanly.
        byte[] corrupted = new byte[]{(byte) 0x8f, (byte) '}'};
        assertThatThrownBy(() -> serializer.fromBinary(corrupted, "wallet-state:v1"))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Corrupted or invalid");
    }

    @Test
    void enums_serializeByNameNotOrdinal() {
        // WriteEnumsUsingName must be on: the JSON carries the enum .name(), never its ordinal.
        String json = new String(
                serializer.toBinary(new WalletEvent.WalletMutated(List.of(leg()), sampleState())), UTF_8);
        assertThat(json).contains("BANK_GATEWAY").contains("DEPOSIT").contains("T0");
        assertThat(json).doesNotContain(SettlementDelay.class.getName());
    }

    // ── forward compatibility: value types not (yet) in the protocol ──────────────

    @Test
    void roundTrips_temporalAndDecimalTypes() {
        // BigDecimal / Instant / LocalDateTime are absent from the wallet protocol today, but future
        // schema additions will depend on Fastjson2's default codecs — prove they round-trip through
        // this serializer with no @type / class leakage and stable ISO-8601 / numeric forms.
        ManifestRegistry reg = ManifestRegistry.builder()
                .register("fixture-temporal:v1", TemporalFixture.class)
                .build();
        FastJsonSerializer typed = new FastJsonSerializer(null, reg, Map.of());

        BigDecimal amount = new BigDecimal("12345.67890");
        TemporalFixture original = new TemporalFixture(
                amount,
                Instant.parse("2026-07-27T10:15:30.00Z"),
                LocalDateTime.of(2026, 7, 27, 13, 0, 0),
                UUID.randomUUID(),
                List.of(amount, BigDecimal.ONE));

        byte[] bytes = typed.toBinary(original);
        String json = new String(bytes, UTF_8);
        TemporalFixture back = (TemporalFixture) typed.fromBinary(bytes, typed.manifest(original));

        // Temporal + UUID types round-trip with exact equality.
        assertThat(back.at()).isEqualTo(original.at());
        assertThat(back.local()).isEqualTo(original.local());
        assertThat(back.id()).isEqualTo(original.id());
        // BigDecimal equality is scale-sensitive; compare by value to tolerate codec scale choices.
        assertThat(back.amount().compareTo(original.amount())).isZero();
        assertThat(back.amounts()).hasSize(original.amounts().size());
        assertThat(back.amounts().get(0).compareTo(original.amounts().get(0))).isZero();
        // Stable on-the-wire forms + no class/type leakage. (Fastjson2 formats Instant with a 'T'
        // and LocalDateTime with a space separator — assert the date/time components, not the sep.)
        assertThat(json).contains("2026-07-27").contains("10:15:30").contains("13:00:00");
        assertThat(json).doesNotContain("@type").doesNotContain("ir.ebb.");
        assertThat(typed.manifest(original)).isEqualTo("fixture-temporal:v1");
    }
}
