package ir.ebb.common.dto.request;

/**
 * Replaces Spring Data's {@code org.springframework.data.domain.PageRequest}.
 * Immutable pagination + sort descriptor consumed by the JDBC query services.
 */
public record PageRequest(int page, int size, String sortProperty, Direction sortDirection) {

    public static PageRequest of(int page, int size, Direction direction, String sortProperty) {
        return new PageRequest(page, size, sortProperty, direction);
    }

    public long offset() {
        return (long) page * size;
    }
}
