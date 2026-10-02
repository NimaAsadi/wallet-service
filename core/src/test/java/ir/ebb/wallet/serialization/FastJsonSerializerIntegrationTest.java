package ir.ebb.wallet.serialization;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.actor.command.Deposit;
import ir.ebb.wallet.aggregate.WalletAggregate;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import ir.ebb.wallet.valueobject.WalletDebt;
import org.apache.pekko.Done;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.pattern.StatusReply;
import org.apache.pekko.serialization.Serialization;
import org.apache.pekko.serialization.SerializationExtension;
import org.apache.pekko.serialization.Serializer;
import org.apache.pekko.serialization.SerializerWithStringManifest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.NotSerializableException;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link FastJsonSerializer} driven through the real Pekko
 * {@link SerializationExtension}: the serializer is resolved from the config binding (serializer
 * id {@code 700001} bound to {@code WalletSerializable}), not constructed by hand. Covers the
 * {@code ActorRef} round-trip path (the one thing the pure unit tests can't exercise) and proves
 * the identifier + manifest + binding stay wired together. The
 * event/snapshot/recovery serialization path is additionally exercised by {@code WalletActorTest}
 * via the persistence testkit.
 */
class FastJsonSerializerIntegrationTest {

    private ActorSystem<Void> system;
    private Serialization serialization;

    @BeforeEach
    void setUp() {
        // Register the wallet-fastjson binding on a real ActorSystem, mirroring application.conf.
        Config config = ConfigFactory.parseString(
                "pekko.loglevel = OFF\n" +
                "pekko.actor {\n" +
                "  serializers { wallet-fastjson = \"ir.ebb.wallet.serialization.FastJsonSerializer\" }\n" +
                "  serialization-identifiers { wallet-fastjson = 700001 }\n" +
                "  serialization-bindings { \"ir.ebb.wallet.serialization.WalletSerializable\" = wallet-fastjson }\n" +
                "}\n");
        system = ActorSystem.create(Behaviors.empty(), "fastjson-it", config);
        serialization = SerializationExtension.get(Adapter.toClassic(system));
    }

    @AfterEach
    void tearDown() {
        system.terminate();
        system.getWhenTerminated().toCompletableFuture().join();
    }

    @Test
    @SuppressWarnings("unchecked")
    void depositCommand_actorRefRoundTripsAndIsCallable() throws NotSerializableException {
        // Erasure forces the raw-class probe + cast: TestProbe<StatusReply<Done>> has no Class token.
        TestProbe<StatusReply<Done>> probe =
                (TestProbe<StatusReply<Done>>) (TestProbe<?>) TestProbe.create(StatusReply.class, system);
        Deposit original = new Deposit(
                UUID.randomUUID(), new Money(100L), SettlementDelay.T_PLUS_0,
                WalletTransactionType.BANK_GATEWAY, probe.getRef());

        Serializer serializer = serialization.findSerializerFor(original);
        assertThat(serializer).isInstanceOf(FastJsonSerializer.class);
        SerializerWithStringManifest swm = (SerializerWithStringManifest) serializer;

        byte[] bytes = serializer.toBinary(original);
        Deposit back = (Deposit) swm.fromBinary(bytes, swm.manifest(original));

        // Scalar command fields survive the round-trip.
        assertThat(back.trackingId()).isEqualTo(original.trackingId());
        assertThat(back.value()).isEqualTo(original.value());
        assertThat(back.settlementDelay()).isEqualTo(original.settlementDelay());
        assertThat(back.walletTransactionType()).isEqualTo(original.walletTransactionType());
        assertThat(swm.manifest(original)).isEqualTo("actor-cmd-deposit:v1");

        // The ActorRef survived serialization: same path, and it still delivers to the probe —
        // the guarantee that inter-node cluster-sharding delivery depends on.
        assertThat(back.replyTo().path()).isEqualTo(original.replyTo().path());
        StatusReply<Done> ack = StatusReply.Ack();
        back.replyTo().tell(ack);
        probe.expectMessage(ack);
    }

    @Test
    void serializationExtension_roundTripsThroughPekkoBinding() throws NotSerializableException {
        // A pure-data state (no ActorRef) round-trips via the real SerializationExtension, proving
        // the config wiring (serializer id + manifest + class) resolves and holds.
        WalletAggregate aggregate = new WalletAggregate(
                UUID.randomUUID(),
                new WalletParameter(10L, 0L),
                new WalletParameter(0L, 0L),
                new WalletParameter(0L, 0L),
                0L, 0L, 0L, 0L, 0L,
                new WalletDebt(),
                7L,
                new HashSet<>());

        Serializer serializer = serialization.findSerializerFor(aggregate);
        assertThat(serializer).isInstanceOf(FastJsonSerializer.class);
        assertThat(serializer.identifier()).isEqualTo(SerializationIds.WALLET_FASTJSON);

        SerializerWithStringManifest swm = (SerializerWithStringManifest) serializer;
        String manifest = swm.manifest(aggregate);
        assertThat(manifest).isEqualTo("actor-aggregate:v1");

        byte[] bytes = serializer.toBinary(aggregate);
        Object back = swm.fromBinary(bytes, manifest);
        assertThat(back).usingRecursiveComparison().isEqualTo(aggregate);
    }
}
