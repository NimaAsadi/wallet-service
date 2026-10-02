package ir.ebb.wallet.actor;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.aggregate.WalletAggregate;
import ir.ebb.wallet.actor.command.WalletCommand;
import ir.ebb.wallet.actor.event.WalletEvent;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.pattern.StatusReply;
import org.apache.pekko.persistence.testkit.javadsl.EventSourcedBehaviorTestKit;
import org.apache.pekko.persistence.typed.PersistenceId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the event-sourced {@link WalletActor} in isolation via the Pekko persistence testkit
 * (in-memory journal/snapshot — no DB). Covers command→event→state for the mutation protocol,
 * the {@code GetWallet}/{@code GetBuyingPower} read commands, wallet-not-exist rejection,
 * duplicate-trackingId idempotency, insufficient-balance rejection, and recovery from the journal.
 * The testkit round-trips each persisted event through serialization, so this also exercises the
 * Fastjson2 serializer ({@code ir.ebb.wallet.serialization.FastJsonSerializer}).
 */
class WalletActorTest {

    private static final SettlementDelay T0 = SettlementDelay.T_PLUS_0;
    private static final WalletTransactionType TYPE = WalletTransactionType.BANK_GATEWAY;
    private static final long ACCOUNT = 555L;

    private ActorSystem<Void> system;
    private EventSourcedBehaviorTestKit<WalletCommand, WalletEvent, WalletAggregate> testKit;

    @BeforeEach
    void setUp() {
        // Testkit journal/snapshot + the wallet Fastjson2 serialization binding (no cluster/jdbc).
        // Mirrors wallet-app/application.conf so the testkit round-trip exercises FastJsonSerializer.
        Config config = EventSourcedBehaviorTestKit.config().withFallback(ConfigFactory.parseString(
                "pekko.actor {\n" +
                "  serializers { wallet-fastjson = \"ir.ebb.wallet.serialization.FastJsonSerializer\" }\n" +
                "  serialization-identifiers { wallet-fastjson = 700001 }\n" +
                "  serialization-bindings { \"ir.ebb.wallet.serialization.WalletSerializable\" = wallet-fastjson }\n" +
                "}\n"));
        system = ActorSystem.create(Behaviors.empty(), "wallet-entity-test", config);
        testKit = EventSourcedBehaviorTestKit.create(system,
                ir.ebb.wallet.actor.WalletActor.create(PersistenceId.ofUniqueId(String.valueOf(ACCOUNT))));
    }

    @AfterEach
    void tearDown() {
        system.terminate();
        system.getWhenTerminated().toCompletableFuture().join();
    }

    private void createWallet() {
        testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.CreateWallet(ACCOUNT, ref));
    }

    private void deposit(UUID trackingId, long amount) {
        testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.Deposit(trackingId, new Money(amount), T0, TYPE, ref));
    }

    // ── mutation protocol ──────────────────────────────────────────────────────────

    @Test
    void createWallet_persistsCreatedEventAndZeroedState() {
        var res = testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.CreateWallet(ACCOUNT, ref));
        assertThat(res.reply()).isEqualTo(StatusReply.ack());
        assertThat(res.events()).hasSize(1);
        assertThat(res.events().get(0)).isInstanceOf(ir.ebb.wallet.actor.event.WalletCreated.class);
        assertThat(res.state().getDbsAccountNumber()).isEqualTo(ACCOUNT);
        assertThat(res.state().getCredit()).isZero();
        assertThat(res.state().getT0()).isEqualTo(new WalletParameter());
    }

    @Test
    void deposit_afterCreate_updatesT0Balance() {
        createWallet();
        var res = testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.Deposit(UUID.randomUUID(), new Money(100L), T0, TYPE, ref));
        assertThat(res.reply()).isEqualTo(StatusReply.ack());
        assertThat(res.events().get(0)).isInstanceOf(ir.ebb.wallet.actor.event.Deposited.class);
        assertThat(res.state().getT0().getBalance()).isEqualTo(100L);
    }

    @Test
    void command_beforeCreate_isRejectedAsWalletNotExist() {
        var res = testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.Deposit(UUID.randomUUID(), new Money(100L), T0, TYPE, ref));
assertThat(res.reply().isError()).isTrue();
        assertThat(res.reply().getError())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Wallet not found");
        assertThat(((BusinessException) res.reply().getError()).getCode())
                .isEqualTo(ExceptionConstants.WALLET_NOT_EXIST.getCode());
        assertThat(res.hasNoEvents()).isTrue();
    }

    @Test
    void duplicateTrackingId_isRejected() {
        createWallet();
        UUID trackingId = UUID.randomUUID();
        deposit(trackingId, 100L);
        var res = testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.Deposit(trackingId, new Money(100L), T0, TYPE, ref));
assertThat(res.reply().isError()).isTrue();
        assertThat(res.reply().getError())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate tracking id");
        assertThat(((BusinessException) res.reply().getError()).getCode())
                .isEqualTo(ExceptionConstants.DUPLICATE_TRACKING_ID.getCode());
    }

    @Test
    void freeze_exceedingBalance_isRejected() {
        createWallet();
        var res = testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.Freeze(UUID.randomUUID(), new Money(1000L), T0, TYPE, false, ref));
assertThat(res.reply().isError()).isTrue();
        assertThat(res.reply().getError())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient balance");
        assertThat(((BusinessException) res.reply().getError()).getCode())
                .isEqualTo(ExceptionConstants.INSUFFICIENT_BALANCE.getCode());
    }

    // ── credit op ──────────────────────────────────────────────────────────────────

    @Test
    void addCredit_increasesCreditAndInitialCredit() {
        createWallet();
        var first = testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.AddCredit(UUID.randomUUID(), new Money(250L), ref));
        assertThat(first.events().get(0)).isInstanceOf(ir.ebb.wallet.actor.event.CreditAdded.class);
        assertThat(first.state().getCredit()).isEqualTo(250L);
        assertThat(first.state().getInitialCredit()).isEqualTo(250L);

        var second = testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.AddCredit(UUID.randomUUID(), new Money(50L), ref));
        assertThat(second.state().getCredit()).isEqualTo(300L);
        assertThat(second.state().getInitialCredit()).isEqualTo(300L);
    }

    @Test
    void addCredit_duplicateTrackingId_isRejected() {
        createWallet();
        UUID trackingId = UUID.randomUUID();
        testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.AddCredit(trackingId, new Money(250L), ref));
        var res = testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.AddCredit(trackingId, new Money(250L), ref));
assertThat(res.reply().isError()).isTrue();
        assertThat(res.reply().getError())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate tracking id");
        assertThat(((BusinessException) res.reply().getError()).getCode())
                .isEqualTo(ExceptionConstants.DUPLICATE_TRACKING_ID.getCode());
    }

    @Test
    void money_rejectsNegativeAmounts() {
        // The value object itself guards negatives — the aggregate's validate(AddCredit) check is
        // defense-in-depth that can only fire on hand-built payloads.
        assertThatThrownBy(() -> new Money(-1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Money can't be less than zero");
    }

    // ── read commands ──────────────────────────────────────────────────────────────

    @Test
    void getWallet_returnsSnapshotOfCurrentState() {
        createWallet();
        deposit(UUID.randomUUID(), 100L);

        var res = testKit.runCommand((ActorRef<StatusReply<WalletSnapshot>> ref) ->
                new ir.ebb.wallet.actor.command.GetWallet(ref));
        StatusReply<WalletSnapshot> reply = res.reply();
        WalletSnapshot snapshot = reply.getValue();
        assertThat(snapshot.dbsAccountNumber()).isEqualTo(ACCOUNT);
        assertThat(snapshot.t0()).isEqualTo(new WalletParameter(100L, 0L));
        assertThat(snapshot.credit()).isZero();
    }

    @Test
    void getWallet_beforeCreate_isRejectedAsWalletNotExist() {
        var res = testKit.runCommand((ActorRef<StatusReply<WalletSnapshot>> ref) ->
                new ir.ebb.wallet.actor.command.GetWallet(ref));
assertThat(res.reply().isError()).isTrue();
        assertThat(res.reply().getError())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Wallet not found");
        assertThat(((BusinessException) res.reply().getError()).getCode())
                .isEqualTo(ExceptionConstants.WALLET_NOT_EXIST.getCode());
        assertThat(res.hasNoEvents()).isTrue();
    }

    @Test
    void getBuyingPower_returnsComputedBuyingPower() {
        createWallet();
        deposit(UUID.randomUUID(), 100L);

        var res = testKit.runCommand((ActorRef<StatusReply<BuyingPower>> ref) ->
                new ir.ebb.wallet.actor.command.GetBuyingPower(T0, ref));
        StatusReply<BuyingPower> reply = res.reply();
        BuyingPower buyingPower = reply.getValue();
        assertThat(buyingPower.balance()).isEqualTo(100L);
        assertThat(buyingPower.credit()).isZero();
    }

    // ── recovery ───────────────────────────────────────────────────────────────────

    @Test
    void restart_recoversStateFromJournal() {
        createWallet();
        deposit(UUID.randomUUID(), 100L);
        testKit.runCommand((ActorRef<StatusReply<org.apache.pekko.Done>> ref) ->
                new ir.ebb.wallet.actor.command.AddCredit(UUID.randomUUID(), new Money(250L), ref));

        testKit.restart();

        var res = testKit.runCommand((ActorRef<StatusReply<WalletSnapshot>> ref) ->
                new ir.ebb.wallet.actor.command.GetWallet(ref));
        WalletSnapshot snapshot = res.reply().getValue();
        assertThat(snapshot.t0().getBalance()).isEqualTo(100L);
        assertThat(snapshot.credit()).isEqualTo(250L);
        assertThat(snapshot.initialCredit()).isEqualTo(250L);
    }
}
