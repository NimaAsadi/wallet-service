package ir.ebb.wallet.constant.valueobject;

import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.entity.WalletParameterEmbedded;
import lombok.Data;

@Data
public class WalletParameter {

    private Long balance;
    private Long frozen;

    public WalletParameter() {
        this.balance = 0L;
        this.frozen = 0L;
    }

    public WalletParameter(Long balance, Long frozen) {
        if (frozen < 0) {
            throw new BusinessException("Frozen Balance cannot be lower than 0.", 5010);
        }
        this.balance = balance;
        this.frozen = frozen;
    }

    public WalletParameterEmbedded adapt() {
        return new WalletParameterEmbedded(balance, frozen);
    }
}
