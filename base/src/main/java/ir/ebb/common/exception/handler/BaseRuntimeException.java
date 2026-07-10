package ir.ebb.common.exception.handler;

import ir.ebb.common.exception.BaseExceptionMapper;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR (unchecked base exception).
 * Carries a numeric domain {@code code} (surfaced in the {@code ErrorResponse}
 * body) and optional i18n {@code args}. Thrown at the application/HTTP boundary.
 */
public class BaseRuntimeException extends RuntimeException {

    private final Integer code;
    private final Object[] args;

    public BaseRuntimeException(String message, Integer code) {
        super(message);
        this.code = code;
        this.args = null;
    }

    public BaseRuntimeException(String message, Integer code, Object... args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    public BaseRuntimeException(BaseExceptionMapper mapper) {
        super(mapper.getMessage());
        this.code = mapper.getCode();
        this.args = null;
    }

    public BaseRuntimeException(BaseExceptionMapper mapper, Object... args) {
        super(mapper.getMessage());
        this.code = mapper.getCode();
        this.args = args;
    }

    public Integer getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
