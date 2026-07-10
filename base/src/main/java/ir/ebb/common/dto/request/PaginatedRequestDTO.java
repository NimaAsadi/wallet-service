package ir.ebb.common.dto.request;

import lombok.Data;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR. Base of all
 * {@code *SearchRequestDTO} beans. Carries pagination + a single sort property.
 */
@Data
public class PaginatedRequestDTO {
    private int page = 0;
    private int size = 50;
    private String orderByProperty = "createdAt";
    private Direction orderByDirection = Direction.DESC;

    public PageRequest toPageRequest() {
        return new PageRequest(page, size, orderByProperty, orderByDirection);
    }
}
