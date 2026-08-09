package ir.ebb.wallet.serialization;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.wallet.WalletCommand;
import ir.ebb.wallet.wallet.WalletReply;
import ir.ebb.wallet.wallet.WalletState;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.serialization.Serialization;
import org.apache.pekko.serialization.SerializationExtension;
import org.apache.pekko.serialization.Serializer;
import org.apache.pekko.serialization.SerializerWithStringManifest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.NotSerializableException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link FastJsonSerializer} driven through the real Pekko
 * {@link SerializationExtension}: the serializer is resolved from the {@code application.conf}
 * binding (serializer id {@code 700001} bound to {@code WalletSerializable}), not constructed by
 * hand. Covers the {@link ActorRef} round-trip path (the one thing the pure unit tests can't
 * exercise) and proves the identifier + manifest + binding stay wired together. The
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
                "  serialization-bindings { \"ir.ebb.wallet.wallet.WalletSerializable\" = wallet-fastjson }\n" +
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
    void actorRef_roundTripsAndIsCallable() throws NotSerializableException {
        TestProbe<WalletReply> probe = TestProbe.create(WalletReply.class, system);
        WalletCommand.Deposit original = new WalletCommand.Deposit(
                UUID.randomUUID(), 555L, 100L, SettlementDelay.T_PLUS_0, WalletTransactionType.BANK_GATEWAY,
                probe.getRef());

        Serializer serializer = serialization.findSerializerFor(original);
        assertThat(serializer).isInstanceOf(FastJsonSerializer.class);
        SerializerWithStringManifest swm = (SerializerWithStringManifest) serializer;

        byte[] bytes = serializer.toBinary(original);
        WalletCommand.Deposit back = (WalletCommand.Deposit) swm.fromBinary(bytes, swm.manifest(original));

        // Scalar command fields survive the round-trip.
        assertThat(back.trackingId()).isEqualTo(original.trackingId());
        assertThat(back.accountNumber()).isEqualTo(original.accountNumber());
        assertThat(back.amount()).isEqualTo(original.amount());
        assertThat(back.type()).isEqualTo(original.type());

        // The ActorRef survived serialization: same path, and it still delivers to the probe —
        // the guarantee that inter-node cluster-sharding delivery depends on.
        assertThat(back.replyTo().path()).isEqualTo(original.replyTo().path());
        WalletReply.Rejected reply = new WalletReply.Rejected("ping", 1);
        back.replyTo().tell(reply);
        probe.expectMessage(reply);
    }

    @Test
    void serializationExtension_roundTripsThroughPekkoBinding() throws NotSerializableException {
        // A pure-data state (no ActorRef) round-trips via the real SerializationExtension, proving
        // the application.conf wiring (serializer id + manifest + class) resolves and holds.
        WalletState state = new WalletState(
                UUID.randomUUID(), 7L,
                User.of(UUID.randomUUID(), 7L),
                new WalletState.Tier(10L, 0L),
                new WalletState.Tier(0L, 0L),
                new WalletState.Tier(0L, 0L),
                0L, 0L, 0L, 0L,
                new WalletState.Debt(0L, 0L, 0L, 0L, 0L, 0L, 0L),
                List.of());

        Serializer serializer = serialization.findSerializerFor(state);
        assertThat(serializer).isInstanceOf(FastJsonSerializer.class);
        assertThat(serializer.identifier()).isEqualTo(SerializationIds.WALLET_FASTJSON);

        SerializerWithStringManifest swm = (SerializerWithStringManifest) serializer;
        String manifest = swm.manifest(state);
        assertThat(manifest).isEqualTo("wallet-state:v1");

        byte[] bytes = serializer.toBinary(state);
        Object back = swm.fromBinary(bytes, manifest);
        assertThat(back).isEqualTo(state);
    }
}
