package ir.ebb.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR. Success envelope for
 * every non-paginated HTTP response: {@code {message, status, data}}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse<T> {
    private String message;
    private int status;
    private T data;
}
