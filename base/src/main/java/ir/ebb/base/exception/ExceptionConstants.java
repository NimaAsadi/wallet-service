package ir.ebb.base.exception;

import ir.ebb.common.exception.BaseExceptionMapper;

public enum ExceptionConstants implements BaseExceptionMapper {

    WALLET_NOT_EXIST("Wallet not found", 4001),
    USER_NOT_EXISTS("User not found", 9000),
    NOT_ACCEPTABLE("Not acceptable", 4002),
    TURNOVER_RESPONSE_TO_LONG("Turnover response is too long", 4003),
    INTERNAL_SERVER_ERROR("Internal server error", 5000),
    INSUFFICIENT_BALANCE("Insufficient balance", 4005),
    ORDER_VALUE_EXCEEDED_BALANCE("Order value exceeded balance", 4006),
    DUPLICATE_TRACKING_ID("Duplicate tracking id", 4007),
    SEPAR_CREDIT_DEBT("exception.separcredit.user.debt", 90007),
    INVALID_COMMAND("exception.invalid.command", 90008),
    ;

    private final String messageKey;
    private final Integer code;

    ExceptionConstants(String messageKey, Integer code) {
        this.messageKey = messageKey;
        this.code = code;
    }

    @Override
    public String getMessage() {
        return this.messageKey;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }


}
