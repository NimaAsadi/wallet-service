package ir.ebb.common.dto.response;

import java.util.List;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR. Error envelope returned
 * for all failure responses: {@code {errors:[{message, code}]}}.
 */
public record BaseErrorResponse(List<ErrorResponse> errors) {
    public BaseErrorResponse(ErrorResponse error) {
        this(List.of(error));
    }
}
