package ir.ebb.common.exception.handler;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR.
 * Checked domain violation thrown from inside aggregates and services
 * (e.g. insufficient balance). Caught at the actor/app boundary and rethrown
 * as a {@link BusinessException} for the HTTP/gRPC layer.
 */
public class ApplicationException extends BaseException {

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Integer status) {
        super(message, status);
    }

    public ApplicationException(String message, Throwable cause, Integer status) {
        super(message, cause, status);
    }
}
