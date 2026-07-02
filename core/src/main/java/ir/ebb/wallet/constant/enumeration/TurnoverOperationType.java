package ir.ebb.wallet.constant.enumeration;

import java.util.List;

public enum TurnoverOperationType {
    BUY,
    SELL,
    DEPOSIT,
    WITHDRAW,
    REMAINING,
    NOT_PROVIDED;

    public static final List<TurnoverOperationType> clientTypes = List.of(BUY, SELL, DEPOSIT, WITHDRAW);
}
