package ir.ebb.common.exception.handler;

import ir.ebb.common.exception.BaseExceptionMapper;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR.
 * Unchecked "not found" exception. The HTTP layer maps it to 404 with a body of
 * {@code BaseErrorResponse{errors:[{message, code}]}}.
 */
public class NotFoundException extends BaseRuntimeException {

    public NotFoundException(BaseExceptionMapper mapper) {
        super(mapper);
    }

    public NotFoundException(String message, Integer code) {
        super(message, code);
    }
}
