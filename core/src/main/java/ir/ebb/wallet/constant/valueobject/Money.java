package ir.ebb.wallet.constant.valueobject;

import ir.ebb.common.exception.handler.BusinessException;

public record Money(Long value) {

    public Money {
        if (value < 0) {
            throw new BusinessException("Money can't be less than zero", 5500);
        }
    }
}
