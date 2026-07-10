package ir.ebb.common.dto.response;

import java.util.List;

/**
 * Replaces Spring Data's {@code org.springframework.data.domain.Page<T>}.
 * A page of results plus total count and pagination metadata.
 */
public record Page<T>(List<T> content, long total, int page, int size) {

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
