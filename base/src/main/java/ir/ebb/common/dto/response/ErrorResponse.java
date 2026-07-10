package ir.ebb.common.dto.response;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR. Single error entry:
 * {@code message} is the (i18n) message key, {@code code} is the domain code.
 */
public record ErrorResponse(String message, Integer code) {
}
