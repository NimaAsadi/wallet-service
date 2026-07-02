package ir.ebb.wallet.repository;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.aggregate.Wallet;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepositoryCustom {

    private static final String UPDATE_WALLET = """
            UPDATE wallet SET
                t0_balance = :t0Balance, t0_frozen = :t0Frozen,
                t1_balance = :t1Balance, t1_frozen = :t1Frozen,
                t2_balance = :t2Balance, t2_frozen = :t2Frozen,
                credit = :credit, initial_credit = :initialCredit,
                separ_credit = :separCredit, separ_initial_credit = :separInitialCredit,
                updated_at = now(),
                version = version + 1
            WHERE id = :id AND version = :version
            """;

    private static final String UPDATE_WALLET_DEBT = """
            UPDATE wallet_debt SET
                t2_to_t0_debt = :t2Tot0Debt,
                t2_to_t1_debt = :t2Tot1Debt,
                t1_to_t0_debt = :t1Tot0Debt,
                t2_to_credit_debt = :t2ToCreditDebt,
                t1_to_credit_debt = :t1ToCreditDebt,
                t2_to_separ_credit_debt = :t2ToSeparCreditDebt,
                t1_to_separ_credit_debt = :t1ToSeparCreditDebt,
                updated_at = now()
            WHERE wallet_id = :walletId
            """;

    private final EntityManager entityManager;

    @Override
    public int updateWalletNative(Wallet wallet) {
        return entityManager.createNativeQuery(UPDATE_WALLET)
                .setParameter("id", wallet.getId())
                .setParameter("version", wallet.getVersion())
                .setParameter("t0Balance", wallet.getT0().getBalance())
                .setParameter("t0Frozen", wallet.getT0().getFrozen())
                .setParameter("t1Balance", wallet.getT1().getBalance())
                .setParameter("t1Frozen", wallet.getT1().getFrozen())
                .setParameter("t2Balance", wallet.getT2().getBalance())
                .setParameter("t2Frozen", wallet.getT2().getFrozen())
                .setParameter("credit", wallet.getCredit())
                .setParameter("initialCredit", wallet.getInitialCredit())
                .setParameter("separCredit", wallet.getSeparCredit())
                .setParameter("separInitialCredit", wallet.getSeparInitialCredit())
                .executeUpdate();
    }

    @Override
    public void updateWalletDebtNative(Wallet wallet) {
        entityManager.createNativeQuery(UPDATE_WALLET_DEBT)
                .setParameter("walletId", wallet.getId())
                .setParameter("t2Tot0Debt", wallet.getWalletDebt().getT2Tot0Debt())
                .setParameter("t2Tot1Debt", wallet.getWalletDebt().getT2Tot1Debt())
                .setParameter("t1Tot0Debt", wallet.getWalletDebt().getT1Tot0Debt())
                .setParameter("t2ToCreditDebt", wallet.getWalletDebt().getT2ToCreditDebt())
                .setParameter("t1ToCreditDebt", wallet.getWalletDebt().getT1ToCreditDebt())
                .setParameter("t2ToSeparCreditDebt", wallet.getWalletDebt().getT2ToSeparCreditDebt())
                .setParameter("t1ToSeparCreditDebt", wallet.getWalletDebt().getT1ToSeparCreditDebt())
                .executeUpdate();
    }
}
