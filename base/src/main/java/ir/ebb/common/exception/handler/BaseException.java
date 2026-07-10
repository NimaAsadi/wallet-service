package ir.ebb.common.exception.handler;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR (checked base exception).
 * Carries an optional {@code status}. Domain violations thrown inside aggregates
 * and services extend this (see {@link ApplicationException}).
 */
public class BaseException extends Exception {

    private final Integer status;

    public BaseException(String message) {
        super(message);
        this.status = null;
    }

    public BaseException(String message, Integer status) {
        super(message);
        this.status = status;
    }

    public BaseException(String message, Throwable cause, Integer status) {
        super(message, cause);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
}
