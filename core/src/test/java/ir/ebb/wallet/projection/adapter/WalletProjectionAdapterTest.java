package ir.ebb.wallet.projection.adapter;

import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.projection.entity.WalletDebtEntity;
import ir.ebb.wallet.projection.entity.WalletEntity;
import ir.ebb.wallet.projection.entity.WalletTransactionEntity;
import ir.ebb.wallet.valueobject.Wallet;
import ir.ebb.wallet.valueobject.WalletDebt;
import ir.ebb.wallet.valueobject.WalletTransaction;
import org.junit.jupiter.api.Test;

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
        assertThat(entity.version).isZero();
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

        WalletTransactionEntity entity = WalletProjectionAdapter.adapt(transaction, TX_ID, USER_ID);

        assertThat(entity.id).isEqualTo(TX_ID);
        assertThat(entity.userId).isEqualTo(USER_ID);
        assertThat(entity.version).isZero();
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

        WalletTransactionEntity entity = WalletProjectionAdapter.adapt(transaction, TX_ID, USER_ID);

        assertThat(entity.frozenBefore).isNull();
        assertThat(entity.frozenAfter).isNull();
        assertThat(entity.balanceBefore).isNull();
        assertThat(entity.balanceAfter).isNull();
        assertThat(entity.trackingId).isEqualTo(TRACKING_ID);
    }

    @Test
    void adaptRejectsNullSources() {
        assertThatThrownBy(() -> WalletProjectionAdapter.adapt((Wallet) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> WalletProjectionAdapter.adapt((WalletDebt) null, WALLET_ID))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> WalletProjectionAdapter.adapt((WalletTransaction) null, TX_ID, USER_ID))
                .isInstanceOf(NullPointerException.class);
    }
}
