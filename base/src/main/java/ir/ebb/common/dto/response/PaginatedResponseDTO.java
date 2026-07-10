package ir.ebb.common.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR. Paginated envelope:
 * {@code {result, total, totalPages, page, size, hasNext, hasPrevious}}.
 * The mapping constructor replaces the old Spring-Data {@code Page}-based one.
 */
@Data
@NoArgsConstructor
public class PaginatedResponseDTO<T> {
    private List<T> result;
    private long total;
    private int totalPages;
    private int page;
    private int size;
    private boolean hasNext;
    private boolean hasPrevious;

    public <R> PaginatedResponseDTO(Page<R> page, Function<R, T> mapper) {
        this.result = page.content().stream().map(mapper).collect(Collectors.toList());
        this.total = page.total();
        this.totalPages = page.totalPages();
        this.page = page.page();
        this.size = page.size();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }
}
