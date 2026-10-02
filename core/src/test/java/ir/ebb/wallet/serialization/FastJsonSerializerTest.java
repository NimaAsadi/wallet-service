package ir.ebb.wallet.serialization;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.actor.WalletSnapshot;
import ir.ebb.wallet.actor.command.AddCredit;
import ir.ebb.wallet.actor.command.GetBuyingPower;
import ir.ebb.wallet.actor.command.GetWallet;
import ir.ebb.wallet.actor.event.CreditAdded;
import ir.ebb.wallet.actor.event.Deposited;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import ir.ebb.wallet.valueobject.WalletDebt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
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

    private ir.ebb.wallet.aggregate.WalletAggregate populatedAggregate() {
        var debt = new WalletDebt();
        debt.setT2Tot0Debt(1L);
        debt.setT1Tot0Debt(2L);
        debt.setT2ToCreditDebt(3L);
        debt.setT1ToSeparCreditDebt(4L);
        return new ir.ebb.wallet.aggregate.WalletAggregate(
                UUID.randomUUID(),
                new WalletParameter(100L, 20L),
                new WalletParameter(50L, 0L),
                new WalletParameter(0L, 0L),
                200L, 999L, 30L, 30L, 30L,
                debt,
                1234567890L,
                new HashSet<>(List.of(UUID.randomUUID(), UUID.randomUUID())));
    }

    private Deposited deposited() {
        return new Deposited(UUID.randomUUID(), new Money(100L),
                SettlementDelay.T_PLUS_0, WalletTransactionType.BANK_GATEWAY, 999L);
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
        // WalletCreated embeds the aggregate, which has identity equals (no Lombok @Data) —
        // compare its content recursively instead of relying on equals.
        var created = new ir.ebb.wallet.actor.event.WalletCreated(populatedAggregate());
        byte[] createdBytes = serializer.toBinary(created);
        var createdBack = (ir.ebb.wallet.actor.event.WalletCreated)
                serializer.fromBinary(createdBytes, serializer.manifest(created));
        assertThat(createdBack.wallet())
                .usingRecursiveComparison()
                .isEqualTo(created.wallet());

        assertRoundTrip(deposited());
        assertRoundTrip(new ir.ebb.wallet.actor.event.Frozen(UUID.randomUUID(), new Money(50L),
                SettlementDelay.T_PLUS_2, WalletTransactionType.ENTRY_ORDER, false, 999L));
        assertRoundTrip(new ir.ebb.wallet.actor.event.Unfrozen(UUID.randomUUID(), new Money(50L),
                SettlementDelay.T_PLUS_2, WalletTransactionType.USER_CANCEL_ORDER, false, 999L));
        assertRoundTrip(new ir.ebb.wallet.actor.event.Spent(UUID.randomUUID(), new Money(50L),
                SettlementDelay.T_PLUS_2, WalletTransactionType.COMPLETE_TRADE, false, 999L));
        assertRoundTrip(new ir.ebb.wallet.actor.event.Withdrew(UUID.randomUUID(), new Money(10L),
                SettlementDelay.T_PLUS_0, WalletTransactionType.USER_WITHDRAWAL, 999L));
        assertRoundTrip(new CreditAdded(UUID.randomUUID(), new Money(250L), 999L));
    }

    @Test
    void roundTrips_walletAggregate() {
        var original = populatedAggregate();
        byte[] bytes = serializer.toBinary(original);
        var back = (ir.ebb.wallet.aggregate.WalletAggregate)
                serializer.fromBinary(bytes, serializer.manifest(original));
        assertThat(back).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void roundTrips_readReplies() {
        var aggregate = populatedAggregate();
        assertRoundTrip(aggregate.toSnapshot());
        assertRoundTrip(new BuyingPower(100L, 200L, 30L));
    }

    @Test
    void roundTrips_commandsWithoutActorRef() {
        // replyTo is null here (no actor system); the round-trip must keep it null, not fail.
        UUID trackingId = UUID.randomUUID();
        assertRoundTrip(new AddCredit(trackingId, new Money(250L), null));
        assertRoundTrip(new GetWallet(null));
        assertRoundTrip(new GetBuyingPower(SettlementDelay.T_PLUS_1, null));
    }

    // ── manifest stability (no class names; versioned; stable forever) ────────────

    @Test
    void manifest_returnsStableVersionedLiterals() {
        assertThat(serializer.manifest(new ir.ebb.wallet.actor.event.WalletCreated(populatedAggregate())))
                .isEqualTo("actor-created:v1");
        assertThat(serializer.manifest(deposited())).isEqualTo("actor-deposited:v1");
        assertThat(serializer.manifest(new CreditAdded(UUID.randomUUID(), new Money(1L), 1L)))
                .isEqualTo("actor-credit-added:v1");
        assertThat(serializer.manifest(populatedAggregate())).isEqualTo("actor-aggregate:v1");
        assertThat(serializer.manifest(new AddCredit(UUID.randomUUID(), new Money(1L), null)))
                .isEqualTo("actor-cmd-add-credit:v1");
        assertThat(serializer.manifest(new GetWallet(null))).isEqualTo("actor-cmd-get-wallet:v1");
        assertThat(serializer.manifest(new GetBuyingPower(SettlementDelay.T_PLUS_0, null)))
                .isEqualTo("actor-cmd-get-buying-power:v1");
        assertThat(serializer.manifest(populatedAggregate().toSnapshot())).isEqualTo("actor-snapshot:v1");
        assertThat(serializer.manifest(new BuyingPower(1L, 2L, 3L))).isEqualTo("actor-buying-power:v1");
        // no manifest must ever contain a Java class name
        String stateJson = new String(serializer.toBinary(populatedAggregate()), UTF_8);
        assertThat(stateJson).doesNotContain("@type").doesNotContain("ir.ebb.");
    }

    // ── schema evolution ───────────────────────────────────────────────────────────

    @Test
    void versionMigration_transformsOldShapeBeforeBinding() {
        // Registry where the CURRENT schema is v2; v1 is reachable only via migration.
        ManifestRegistry regV2 = ManifestRegistry.builder()
                .register("actor-aggregate:v2", ir.ebb.wallet.aggregate.WalletAggregate.class)
                .build();
        // Build v1-shape bytes: a real WalletAggregate serialized, then "credit" renamed to a
        // legacy key "crd" — simulating an old persisted schema.
        var aggregate = populatedAggregate();
        FastJsonSerializer v2Writer = new FastJsonSerializer(null, regV2, Map.of());
        JSONObject node = JSON.parseObject(new String(v2Writer.toBinary(aggregate), UTF_8));
        node.put("crd", node.get("credit"));
        node.remove("credit");
        byte[] v1Bytes = node.toString().getBytes(UTF_8);

        // Migration v1 -> v2 restores the current field name.
        Migration rename = new Migration() {
            @Override public String fromManifest() { return "actor-aggregate:v1"; }
            @Override public String toManifest() { return "actor-aggregate:v2"; }
            @Override public JSONObject migrate(JSONObject old) {
                if (old.containsKey("crd")) {
                    old.put("credit", old.get("crd"));
                    old.remove("crd");
                }
                return old;
            }
        };
        FastJsonSerializer migratable = new FastJsonSerializer(null, regV2, Map.of(rename.fromManifest(), rename));

        ir.ebb.wallet.aggregate.WalletAggregate recovered =
                (ir.ebb.wallet.aggregate.WalletAggregate) migratable.fromBinary(v1Bytes, "actor-aggregate:v1");
        assertThat(recovered.getCredit()).isEqualTo(200L);

        // Without the migration, v1 is an unknown manifest (not in the registry) and is rejected.
        assertThatThrownBy(() -> v2Writer.fromBinary(v1Bytes, "actor-aggregate:v1"))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Unknown manifest");
    }

    @Test
    void missingFields_defaultRatherThanFail() {
        // Forward/backward compatibility: a payload missing fields yields nulls — deserialization
        // does not fail (a record component absent from the JSON stays null).
        byte[] minimal = ("{\"trackingId\":\"" + UUID.randomUUID() + "\"}").getBytes(UTF_8);
        CreditAdded partial = (CreditAdded) serializer.fromBinary(minimal, "actor-credit-added:v1");
        assertThat(partial.value()).isNull();
        assertThat(partial.dbsAccountNumber()).isNull();
    }

    @Test
    void additionalFields_areIgnored() {
        var aggregate = populatedAggregate();
        JSONObject node = JSON.parseObject(new String(serializer.toBinary(aggregate), UTF_8));
        node.put("futureField", 42); // a field added by a newer writer
        byte[] bytes = node.toString().getBytes(UTF_8);

        var recovered = (ir.ebb.wallet.aggregate.WalletAggregate) serializer.fromBinary(bytes, "actor-aggregate:v1");
        assertThat(recovered).usingRecursiveComparison().isEqualTo(aggregate); // extra field tolerated
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
        assertThatThrownBy(() -> serializer.fromBinary("{not json".getBytes(UTF_8), "actor-aggregate:v1"))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Corrupted or invalid");
    }

    @Test
    void corruptedPayload_throwsSerializationException() {
        // 0x8f is not a valid UTF-8 leading byte; decoding then parsing must fail cleanly.
        byte[] corrupted = new byte[]{(byte) 0x8f, (byte) '}'};
        assertThatThrownBy(() -> serializer.fromBinary(corrupted, "actor-aggregate:v1"))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Corrupted or invalid");
    }

    @Test
    void enums_serializeByNameNotOrdinal() {
        // WriteEnumsUsingName must be on: the JSON carries the enum .name(), never its ordinal.
        String json = new String(serializer.toBinary(deposited()), UTF_8);
        assertThat(json).contains("BANK_GATEWAY").contains("T_PLUS_0");
        assertThat(json).doesNotContain(SettlementDelay.class.getName());
    }

    // ── WalletCreated embeds the event-sourced state (WalletAggregate) ─────────────

    @Test
    void roundTrips_actorWalletCreatedWithPopulatedAggregate() {
        var aggregate = populatedAggregate();
        var original = new ir.ebb.wallet.actor.event.WalletCreated(aggregate);

        String manifest = serializer.manifest(original);
        assertThat(manifest).isEqualTo("actor-created:v1");

        byte[] bytes = serializer.toBinary(original);
        String json = new String(bytes, UTF_8);
        assertThat(json).contains("dbsAccountNumber")
                .doesNotContain("@type")
                .doesNotContain("ir.ebb.");

        var back = (ir.ebb.wallet.actor.event.WalletCreated) serializer.fromBinary(bytes, manifest);
        var state = back.wallet();
        assertThat(state.getId()).isEqualTo(aggregate.getId());
        assertThat(state.getDbsAccountNumber()).isEqualTo(1234567890L);
        assertThat(state.getCredit()).isEqualTo(200L);
        assertThat(state.getBuyingPower()).isEqualTo(999L);
        assertThat(state.getInitialCredit()).isEqualTo(30L);
        assertThat(state.getSeparCredit()).isEqualTo(30L);
        assertThat(state.getSeparInitialCredit()).isEqualTo(30L);
        assertThat(state.getT0()).isEqualTo(new WalletParameter(100L, 20L));
        assertThat(state.getT1()).isEqualTo(new WalletParameter(50L, 0L));
        assertThat(state.getT2()).isEqualTo(new WalletParameter(0L, 0L));
        assertThat(state.getWalletDebt()).isEqualTo(aggregate.getWalletDebt());
        assertThat(state.getTrackingIds())
                .containsExactlyInAnyOrderElementsOf(aggregate.getTrackingIds());
    }

    @Test
    void actorWalletCreated_withoutTrackingIds_yieldsNonNullSet() {
        // The field initializer must hold: a payload without trackingIds (older writer, hand-edit)
        // deserializes to an empty set, never null — WalletAggregate.applyEvent calls
        // trackingIds.add on every event, and a null set would NPE the actor.
        byte[] bytes = ("{\"wallet\":{\"id\":\"" + UUID.randomUUID() + "\",\"dbsAccountNumber\":42}}")
                .getBytes(UTF_8);

        var back = (ir.ebb.wallet.actor.event.WalletCreated)
                serializer.fromBinary(bytes, "actor-created:v1");

        assertThat(back.wallet().getTrackingIds()).isNotNull().isEmpty();
        assertThat(back.wallet().getDbsAccountNumber()).isEqualTo(42L);
    }

    @Test
    void snapshot_roundTripsWithDeepCopiedTiers() {
        // WalletSnapshot must survive the wire with its tier values intact — the aggregate's live
        // WalletParameter instances are copied at snapshot time and re-created on read.
        WalletSnapshot snapshot = populatedAggregate().toSnapshot();

        byte[] bytes = serializer.toBinary(snapshot);
        WalletSnapshot back = (WalletSnapshot) serializer.fromBinary(bytes, "actor-snapshot:v1");

        assertThat(back.t0()).isEqualTo(new WalletParameter(100L, 20L));
        assertThat(back.t1()).isEqualTo(new WalletParameter(50L, 0L));
        assertThat(back.t2()).isEqualTo(new WalletParameter(0L, 0L));
        assertThat(back.credit()).isEqualTo(200L);
        assertThat(back.separInitialCredit()).isEqualTo(30L);
        assertThat(back.dbsAccountNumber()).isEqualTo(1234567890L);
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
