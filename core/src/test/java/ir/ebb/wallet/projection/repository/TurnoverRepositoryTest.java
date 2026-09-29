package ir.ebb.wallet.projection.repository;

import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-mapping tests for {@link TurnoverRepository#buildWhere} — no DB: the WHERE text's
 * {@code $n} numbering and the bind-value order must stay in lockstep, and the to-bound keeps
 * the legacy inclusive end-of-day conversion.
 */
class TurnoverRepositoryTest {

    private final TurnoverRepository repository = new TurnoverRepository();

    @Test
    void emptySpecYieldsNoWhereClause() {
        var where = repository.buildWhere(TurnoverSpecificationDTO.builder().build());
        assertThat(where.sql()).isEmpty();
        assertThat(where.params()).isEmpty();
    }

    @Test
    void fullSpecNumbersPlaceholdersInBindOrder() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 12, 0);

        var where = repository.buildWhere(TurnoverSpecificationDTO.builder()
                .dbsAccountNumber(1234L)
                .fromCreatedAt(from)
                .toCreatedAt(to)
                .build());

        assertThat(where.sql())
                .isEqualTo(" WHERE account_number = $1 AND created_at >= $2 AND created_at <= $3");
        assertThat(where.params()).containsExactly(
                1234L,
                from,
                LocalDateTime.of(2026, 1, 31, 23, 59, 59));
    }

    @Test
    void singleClauseSpecsNumberFromOne() {
        var accountOnly = repository.buildWhere(TurnoverSpecificationDTO.builder().dbsAccountNumber(7L).build());
        assertThat(accountOnly.sql()).isEqualTo(" WHERE account_number = $1");
        assertThat(accountOnly.params()).containsExactly(7L);

        var fromOnly = repository.buildWhere(TurnoverSpecificationDTO.builder()
                .fromCreatedAt(LocalDateTime.of(2026, 3, 1, 8, 0)).build());
        assertThat(fromOnly.sql()).isEqualTo(" WHERE created_at >= $1");
        assertThat(fromOnly.params()).containsExactly(LocalDateTime.of(2026, 3, 1, 8, 0));

        var toOnly = repository.buildWhere(TurnoverSpecificationDTO.builder()
                .toCreatedAt(LocalDateTime.of(2026, 3, 5, 8, 0)).build());
        assertThat(toOnly.sql()).isEqualTo(" WHERE created_at <= $1");
        assertThat(toOnly.params()).containsExactly(LocalDateTime.of(2026, 3, 5, 23, 59, 59));
    }
}
