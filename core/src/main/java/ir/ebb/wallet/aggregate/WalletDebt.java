package ir.ebb.wallet.aggregate;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.entity.WalletDebtEntity;
import lombok.Data;

@Data
public class WalletDebt {

    private Long t2Tot0Debt          = 0L;
    private Long t2Tot1Debt          = 0L;
    private Long t1Tot0Debt          = 0L;
    private Long t2ToCreditDebt      = 0L;
    private Long t1ToCreditDebt      = 0L;
    private Long t2ToSeparCreditDebt = 0L;
    private Long t1ToSeparCreditDebt = 0L;

    /**
     * Records that {@code difference} units of {@code settlementDelay}'s balance were borrowed from {@code lender}.
     */
    public void calculateDebt(SettlementDelay settlementDelay, SettlementDelay lender, long difference) {
        if (difference <= 0) return;
        if (settlementDelay == SettlementDelay.T_PLUS_2 && lender == SettlementDelay.T_PLUS_0) {
            t2Tot0Debt += difference;
        } else if (settlementDelay == SettlementDelay.T_PLUS_2 && lender == SettlementDelay.T_PLUS_1) {
            t2Tot1Debt += difference;
        } else if (settlementDelay == SettlementDelay.T_PLUS_1 && lender == SettlementDelay.T_PLUS_0) {
            t1Tot0Debt += difference;
        }
    }

    /**
     * When settling {@code fromDelay} by depositing into {@code toDelay}, returns how much of
     * {@code currentValue} should be applied to repay debt owed to {@code toDelay}.
     * Updates the debt records accordingly and returns the settled amount.
     */
    public Long calculateDebtSettlement(SettlementDelay fromDelay, SettlementDelay toDelay, Long currentValue) {
        if (fromDelay == SettlementDelay.T_PLUS_1 && toDelay == SettlementDelay.T_PLUS_0) {
            long repay = Math.min(t1Tot0Debt, currentValue);
            t1Tot0Debt -= repay;
            return repay;
        } else if (fromDelay == SettlementDelay.T_PLUS_2 && toDelay == SettlementDelay.T_PLUS_0) {
            long repay = Math.min(t2Tot0Debt, currentValue);
            t2Tot0Debt -= repay;
            return repay;
        } else if (fromDelay == SettlementDelay.T_PLUS_2 && toDelay == SettlementDelay.T_PLUS_1) {
            long repay = Math.min(t2Tot1Debt, currentValue);
            t2Tot1Debt -= repay;
            return repay;
        }
        return 0L;
    }

    public WalletDebtEntity adapt() {
        WalletDebtEntity entity = new WalletDebtEntity();
        entity.setT2Tot0Debt(t2Tot0Debt);
        entity.setT2Tot1Debt(t2Tot1Debt);
        entity.setT1Tot0Debt(t1Tot0Debt);
        entity.setT2ToCreditDebt(t2ToCreditDebt);
        entity.setT1ToCreditDebt(t1ToCreditDebt);
        entity.setT2ToSeparCreditDebt(t2ToSeparCreditDebt);
        entity.setT1ToSeparCreditDebt(t1ToSeparCreditDebt);
        return entity;
    }
}
