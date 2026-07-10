package ir.ebb.wallet.entity;

import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class WalletParameterEmbedded implements Serializable {

    private Long balance = 0L;
    private Long frozen = 0L;

    public WalletParameterEmbedded() {
        if (ObjectUtils.isNotEmpty(frozen) && frozen < 0) {
            throw new BusinessException("Frozen Balance cannot be lower than 0.", 5010);
        }
    }

    public WalletParameter adoptToDomain() {
        return new WalletParameter(balance, frozen);
    }
}
