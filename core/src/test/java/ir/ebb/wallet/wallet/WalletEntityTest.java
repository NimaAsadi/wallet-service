package ir.ebb.wallet.wallet;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.persistence.testkit.javadsl.EventSourcedBehaviorTestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the event-sourced {@link WalletEntity} in isolation via the Pekko persistence testkit
 * (in-memory journal/snapshot — no DB). Covers command→event→state, wallet-not-exist rejection,
 * duplicate-trackingId idempotency, insufficient-balance rejection, and recovery from the journal.
 * The testkit round-trips each persisted event through serialization, so this also exercises the
 * Jackson CBOR binding.
 */
class WalletEntityTest {

    private static final SettlementDelay T0 = SettlementDelay.T_PLUS_0;
    private static final WalletTransactionType TYPE = WalletTransactionType.BANK_GATEWAY;
    private static final long ACCOUNT = 555L;
    private static final User USER = User.of(UUID.randomUUID(), ACCOUNT);

    private ActorSystem<Void> system;
    private EventSourcedBehaviorTestKit<WalletCommand, WalletEvent, WalletState> testKit;

    @BeforeEach
    void setUp() {
        // Testkit journal/snapshot + the wallet CBOR serialization binding (no cluster/jdbc).
        Config config = EventSourcedBehaviorTestKit.config().withFallback(ConfigFactory.parseString(
                "pekko.actor.serialization-bindings {\n" +
                "  \"ir.ebb.wallet.wallet.WalletSerializable\" = jackson-cbor\n" +
                "}"));
        system = ActorSystem.create(Behaviors.empty(), "wallet-entity-test", config);
        testKit = EventSourcedBehaviorTestKit.create(system, WalletEntity.create(String.valueOf(ACCOUNT)));
    }

    @AfterEach
    void tearDown() {
        system.terminate();
        system.getWhenTerminated().toCompletableFuture().join();
    }

    private void createWallet() {
        testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.CreateWallet(USER, UUID.randomUUID(), ref));
    }

    @Test
    void createWallet_persistsCreatedEventAndEmptyState() {
        var res = testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.CreateWallet(USER, UUID.randomUUID(), ref));
        assertThat(res.reply()).isInstanceOf(WalletReply.Accepted.class);
        assertThat(res.events()).hasSize(1);
        assertThat(res.events().get(0)).isInstanceOf(WalletEvent.WalletCreated.class);
        assertThat(res.state().isCreated()).isTrue();
        assertThat(res.state().accountNumber()).isEqualTo(ACCOUNT);
    }

    @Test
    void deposit_afterCreate_persistsMutatedEventAndUpdatesBalance() {
        createWallet();
        var res = testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.Deposit(UUID.randomUUID(), USER, 100L, T0, TYPE, ref));
        assertThat(res.reply()).isInstanceOf(WalletReply.Accepted.class);
        assertThat(res.events()).hasSize(1);
        assertThat(res.events().get(0)).isInstanceOf(WalletEvent.WalletMutated.class);
        assertThat(res.state().t0().balance()).isEqualTo(100L);
    }

    @Test
    void deposit_beforeCreate_isRejectedAsWalletNotExist() {
        var res = testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.Deposit(UUID.randomUUID(), USER, 100L, T0, TYPE, ref));
        assertThat(res.reply()).isInstanceOf(WalletReply.Rejected.class);
        assertThat(((WalletReply.Rejected) res.reply()).code()).isEqualTo(4001); // WALLET_NOT_EXIST
        assertThat(res.hasNoEvents()).isTrue();
    }

    @Test
    void duplicateTrackingId_isRejected() {
        UUID tracking = UUID.randomUUID();
        createWallet();
        var first = testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.Deposit(tracking, USER, 100L, T0, TYPE, ref));
        assertThat(first.reply()).isInstanceOf(WalletReply.Accepted.class);

        var dup = testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.Deposit(tracking, USER, 50L, T0, TYPE, ref));
        assertThat(dup.reply()).isInstanceOf(WalletReply.Rejected.class);
        assertThat(((WalletReply.Rejected) dup.reply()).code()).isEqualTo(4007); // DUPLICATE_TRACKING_ID
        assertThat(dup.hasNoEvents()).isTrue();
    }

    @Test
    void freeze_exceedingBalance_isRejectedInsufficient() {
        createWallet();
        var res = testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.Freeze(UUID.randomUUID(), USER, 100L, T0, TYPE, false, ref));
        assertThat(res.reply()).isInstanceOf(WalletReply.Rejected.class);
        // domain (ApplicationException) rejections surface as code 4005 from the entity
        assertThat(((WalletReply.Rejected) res.reply()).code()).isEqualTo(4005);
    }

    @Test
    void restart_recoversStateFromJournal() {
        createWallet();
        testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.Deposit(UUID.randomUUID(), USER, 100L, T0, TYPE, ref));
        testKit.runCommand((ActorRef<WalletReply> ref) -> new WalletCommand.Deposit(UUID.randomUUID(), USER, 30L, T0, TYPE, ref));
        WalletState before = testKit.getState();

        EventSourcedBehaviorTestKit.RestartResult<WalletState> recovered = testKit.restart();
        assertThat(recovered.state().t0().balance()).isEqualTo(before.t0().balance());
        assertThat(recovered.state().id()).isEqualTo(before.id());
    }
}
