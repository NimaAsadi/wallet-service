package ir.ebb.wallet.constant.enumeration;

import ir.ebb.common.constant.enumeration.SettlementDelay;

public enum WalletParameterType {
    T0,
    T1,
    T2,
    CREDIT;

    public static WalletParameterType getBySettlementDelay(SettlementDelay settlementDelay) {
        return switch (settlementDelay) {
            case T_PLUS_0 -> T0;
            case T_PLUS_1 -> T1;
            case T_PLUS_2 -> T2;
        };
    }
}
