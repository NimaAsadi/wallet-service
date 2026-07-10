package ir.ebb.common.exception.handler;

import ir.ebb.common.exception.BaseExceptionMapper;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR.
 * Unchecked boundary exception. The HTTP layer maps every BusinessException to
 * HTTP 406 with a body of {@code BaseErrorResponse{errors:[{message, code}]}}
 * (the domain {@code code} goes in the body, NOT as the HTTP status).
 */
public class BusinessException extends BaseRuntimeException {

    public BusinessException(String message, Integer code) {
        super(message, code);
    }

    public BusinessException(String message, Integer code, Object... args) {
        super(message, code, args);
    }

    public BusinessException(BaseExceptionMapper mapper) {
        super(mapper);
    }

    public BusinessException(BaseExceptionMapper mapper, Object... args) {
        super(mapper, args);
    }
}
