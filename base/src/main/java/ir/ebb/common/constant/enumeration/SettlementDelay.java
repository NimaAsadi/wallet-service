package ir.ebb.common.constant.enumeration;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR.
 * Settlement delay tiers used by the wallet aggregate. When a tier is short of
 * funds during a freeze, the wallet borrows from its {@link #getLender()} until
 * it reaches T+0, after which it falls back to spending credit.
 */
public enum SettlementDelay {
    T_PLUS_0,
    T_PLUS_1,
    T_PLUS_2;

    /** The next-lower tier to borrow from, or {@code null} for T+0 (no lower tier). */
    public SettlementDelay getLender() {
        return switch (this) {
            case T_PLUS_2 -> T_PLUS_1;
            case T_PLUS_1 -> T_PLUS_0;
            case T_PLUS_0 -> null;
        };
    }
}
