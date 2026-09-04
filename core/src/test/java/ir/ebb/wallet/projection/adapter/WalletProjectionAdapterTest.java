package ir.ebb.wallet.projection.adapter;

import ir.ebb.wallet.aggregate.WalletAggregate;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import ir.ebb.wallet.projection.entity.WalletDebtEntity;
import ir.ebb.wallet.projection.entity.WalletEntity;
import ir.ebb.wallet.projection.entity.WalletTransactionEntity;
import ir.ebb.wallet.projection.repository.WalletWithDebt;
import ir.ebb.wallet.valueobject.Wallet;
import ir.ebb.wallet.valueobject.WalletDebt;
import ir.ebb.wallet.valueobject.WalletTransaction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure-mapping tests for {@link WalletProjectionAdapter} — no DB, no actor system. Guards the
 * VO → projection-entity conversions the read-model handlers will rely on (tier flattening,
 * debt-counter renames, parameter-sourced id/userId, null tolerance for boxed sources).
 */
class WalletProjectionAdapterTest {

    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TX_ID = UUID.randomUUID();
    private static final UUID TRACKING_ID = UUID.randomUUID();

    @Test
    void adaptWalletMapsIdentityTiersAndCreditColumns() {
        Wallet wallet = new Wallet(1234567L);
        wallet.setId(WALLET_ID);
        wallet.setVersion(7L);
        wallet.getT0().setBalance(100L);
        wallet.getT0().setFrozen(40L);
        wallet.getT1().setBalance(200L);
        wallet.getT1().setFrozen(0L);
        wallet.getT2().setBalance(300L);
        wallet.getT2().setFrozen(60L);
        wallet.setCredit(11L);
        wallet.setInitialCredit(22L);
        wallet.setSeparCredit(33L);
        wallet.setSeparInitialCredit(44L);

        WalletEntity entity = WalletProjectionAdapter.adapt(wallet);

        assertThat(entity.id).isEqualTo(WALLET_ID);
        assertThat(entity.version).isEqualTo(7L);
        assertThat(entity.accountNumber).isEqualTo(1234567L);
        assertThat(entity.t0Balance).isEqualTo(100L);
        assertThat(entity.t0Frozen).isEqualTo(40L);
        assertThat(entity.t1Balance).isEqualTo(200L);
        assertThat(entity.t1Frozen).isZero();
        assertThat(entity.t2Balance).isEqualTo(300L);
        assertThat(entity.t2Frozen).isEqualTo(60L);
        assertThat(entity.credit).isEqualTo(11L);
        assertThat(entity.initialCredit).isEqualTo(22L);
        assertThat(entity.separCredit).isEqualTo(33L);
        assertThat(entity.separInitialCredit).isEqualTo(44L);
        assertThat(entity.createdAt).isNull();
        assertThat(entity.updatedAt).isNull();
    }

    @Test
    void adaptWalletToleratesNullVOFields() {
        Wallet wallet = new Wallet();
        wallet.setT0(null);

        WalletEntity entity = WalletProjectionAdapter.adapt(wallet);

        assertThat(entity.id).isNull();
        assertThat(entity.version).isZero();
        assertThat(entity.accountNumber).isZero();
        assertThat(entity.t0Balance).isZero();
        assertThat(entity.t0Frozen).isZero();
        assertThat(entity.t1Balance).isZero();
        assertThat(entity.t1Frozen).isZero();
        assertThat(entity.t2Balance).isZero();
        assertThat(entity.t2Frozen).isZero();
        assertThat(entity.credit).isZero();
        assertThat(entity.initialCredit).isZero();
        assertThat(entity.separCredit).isZero();
        assertThat(entity.separInitialCredit).isZero();
    }

    @Test
    void adaptWalletDebtRenamesCountersAndThreadsWalletId() {
        WalletDebt debt = new WalletDebt();
        debt.setT2Tot0Debt(5L);
        debt.setT2Tot1Debt(6L);
        debt.setT1Tot0Debt(7L);

        WalletDebtEntity entity = WalletProjectionAdapter.adapt(debt, WALLET_ID);

        assertThat(entity.walletId).isEqualTo(WALLET_ID);
        assertThat(entity.t2ToT0Debt).isEqualTo(5L);
        assertThat(entity.t2ToT1Debt).isEqualTo(6L);
        assertThat(entity.t1ToT0Debt).isEqualTo(7L);
        assertThat(entity.createdAt).isNull();
        assertThat(entity.updatedAt).isNull();
    }

    @Test
    void adaptWalletTransactionCopiesAllColumns() {
        WalletTransaction transaction = WalletTransaction.builder()
                .accountNumber(1234567L)
                .walletId(WALLET_ID)
                .walletOperationType(WalletOperationType.FREEZE)
                .walletTransactionType(WalletTransactionType.BANK_GATEWAY)
                .walletParameterType(WalletParameterType.T0)
                .amount(500L)
                .trackingId(TRACKING_ID)
                .frozenBefore(100L)
                .frozenAfter(600L)
                .balanceBefore(1000L)
                .balanceAfter(1000L)
                .build();

        WalletTransactionEntity entity = WalletProjectionAdapter.adapt(transaction);

        assertThat(entity.accountNumber).isEqualTo(1234567L);
        assertThat(entity.walletId).isEqualTo(WALLET_ID);
        assertThat(entity.walletOperationType).isEqualTo(WalletOperationType.FREEZE);
        assertThat(entity.walletTransactionType).isEqualTo(WalletTransactionType.BANK_GATEWAY);
        assertThat(entity.walletParameterType).isEqualTo(WalletParameterType.T0);
        assertThat(entity.amount).isEqualTo(500L);
        assertThat(entity.trackingId).isEqualTo(TRACKING_ID);
        assertThat(entity.frozenBefore).isEqualTo(100L);
        assertThat(entity.frozenAfter).isEqualTo(600L);
        assertThat(entity.balanceBefore).isEqualTo(1000L);
        assertThat(entity.balanceAfter).isEqualTo(1000L);
        assertThat(entity.createdAt).isNull();
        assertThat(entity.updatedAt).isNull();
    }

    @Test
    void adaptWalletTransactionPreservesNullAuditColumns() {
        // unfreeze legs carry null before/after snapshots; they must not be coerced to 0
        WalletTransaction transaction = WalletTransaction.builder()
                .accountNumber(1234567L)
                .walletId(WALLET_ID)
                .walletOperationType(WalletOperationType.UNFREEZE)
                .walletTransactionType(WalletTransactionType.USER_CANCEL_ORDER)
                .walletParameterType(WalletParameterType.T0)
                .amount(500L)
                .trackingId(TRACKING_ID)
                .build();

        WalletTransactionEntity entity = WalletProjectionAdapter.adapt(transaction);

        assertThat(entity.frozenBefore).isNull();
        assertThat(entity.frozenAfter).isNull();
        assertThat(entity.balanceBefore).isNull();
        assertThat(entity.balanceAfter).isNull();
        assertThat(entity.trackingId).isEqualTo(TRACKING_ID);
    }

    @Test
    void adaptWalletDebtMapsAllSevenCounters() {
        WalletDebt debt = new WalletDebt();
        debt.setT2Tot0Debt(1L);
        debt.setT2Tot1Debt(2L);
        debt.setT1Tot0Debt(3L);
        debt.setT2ToCreditDebt(4L);
        debt.setT1ToCreditDebt(5L);
        debt.setT2ToSeparCreditDebt(6L);
        debt.setT1ToSeparCreditDebt(7L);

        WalletDebtEntity entity = WalletProjectionAdapter.adapt(debt, WALLET_ID);

        // All seven counters must survive: a read-modify-write that dropped any of them would
        // silently zero that column in the read model.
        assertThat(entity.walletId).isEqualTo(WALLET_ID);
        assertThat(entity.t2ToT0Debt).isEqualTo(1L);
        assertThat(entity.t2ToT1Debt).isEqualTo(2L);
        assertThat(entity.t1ToT0Debt).isEqualTo(3L);
        assertThat(entity.t2ToCreditDebt).isEqualTo(4L);
        assertThat(entity.t1ToCreditDebt).isEqualTo(5L);
        assertThat(entity.t2ToSeparCreditDebt).isEqualTo(6L);
        assertThat(entity.t1ToSeparCreditDebt).isEqualTo(7L);
    }

    @Test
    void adaptAggregateMapsDbsAccountNumberAndInitsTransactions() {
        WalletDebt debt = new WalletDebt();
        debt.setT1Tot0Debt(9L);
        WalletAggregate aggregate = new WalletAggregate(
                WALLET_ID,
                new WalletParameter(100L, 10L),
                new WalletParameter(0L, 0L),
                new WalletParameter(0L, 0L),
                5L, 0L, 6L, 7L, 8L,
                debt,
                1234567L,
                new HashSet<>(List.of(TRACKING_ID)));

        Wallet wallet = WalletProjectionAdapter.adapt(aggregate);

        // Regression: accountNumber must come from dbsAccountNumber (it self-assigned 0 before).
        assertThat(wallet.getAccountNumber()).isEqualTo(1234567L);
        assertThat(wallet.getId()).isEqualTo(WALLET_ID);
        assertThat(wallet.getT0().getBalance()).isEqualTo(100L);
        assertThat(wallet.getCredit()).isEqualTo(5L);
        assertThat(wallet.getInitialCredit()).isEqualTo(6L);
        assertThat(wallet.getSeparCredit()).isEqualTo(7L);
        assertThat(wallet.getSeparInitialCredit()).isEqualTo(8L);
        assertThat(wallet.getWalletDebt()).isSameAs(debt);
        // The waterfall appends legs — a null list here NPEs the first deposit.
        assertThat(wallet.getWalletTransactions()).isNotNull().isEmpty();
    }

    @Test
    void adaptWalletWithDebtInitsTransactionsAndMapsVersion() {
        WalletEntity walletEntity = new WalletEntity();
        walletEntity.id = WALLET_ID;
        walletEntity.version = 7L;
        walletEntity.accountNumber = 1234567L;
        walletEntity.t0Balance = 100L;
        WalletDebtEntity debtEntity = new WalletDebtEntity();
        debtEntity.walletId = WALLET_ID;
        debtEntity.t1ToT0Debt = 3L;

        Wallet wallet = WalletProjectionAdapter.adapt(new WalletWithDebt(walletEntity, debtEntity));

        assertThat(wallet.getVersion()).isEqualTo(7L);
        assertThat(wallet.getId()).isEqualTo(WALLET_ID);
        assertThat(wallet.getAccountNumber()).isEqualTo(1234567L);
        assertThat(wallet.getT0().getBalance()).isEqualTo(100L);
        assertThat(wallet.getWalletDebt().getT1Tot0Debt()).isEqualTo(3L);
        assertThat(wallet.getWalletTransactions()).isNotNull().isEmpty();
    }

    @Test
    void adaptWalletTransactionUsesProvidedId() {
        WalletTransaction transaction = WalletTransaction.builder()
                .accountNumber(1234567L)
                .walletId(WALLET_ID)
                .walletOperationType(WalletOperationType.DEPOSIT)
                .walletTransactionType(WalletTransactionType.BANK_GATEWAY)
                .walletParameterType(WalletParameterType.T0)
                .amount(500L)
                .trackingId(TRACKING_ID)
                .balanceBefore(0L)
                .balanceAfter(500L)
                .build();

        WalletTransactionEntity entity = WalletProjectionAdapter.adapt(transaction, TX_ID);

        // Replay idempotency: the handler's deterministic per-event id must be kept, not replaced.
        assertThat(entity.id).isEqualTo(TX_ID);
        assertThat(entity.walletId).isEqualTo(WALLET_ID);
        assertThat(entity.amount).isEqualTo(500L);
        assertThat(entity.balanceAfter).isEqualTo(500L);
    }

    @Test
    void adaptRejectsNullSources() {
        assertThatThrownBy(() -> WalletProjectionAdapter.adapt((Wallet) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> WalletProjectionAdapter.adapt((WalletDebt) null, WALLET_ID))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> WalletProjectionAdapter.adapt((WalletTransaction) null))
                .isInstanceOf(NullPointerException.class);
    }
}
