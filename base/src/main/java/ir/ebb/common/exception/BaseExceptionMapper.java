package ir.ebb.common.exception;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR.
 * Implemented by {@link ir.ebb.base.exception.ExceptionConstants} to expose a
 * message key and numeric domain code for each error.
 */
public interface BaseExceptionMapper {
    String getMessage();
    Integer getCode();
}
