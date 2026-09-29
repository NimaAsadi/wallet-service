package ir.ebb.wallet.projection.repository;

import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-mapping tests for {@link CreditHistoryRepository#buildWhere} — no DB: the WHERE text's
 * {@code $n} numbering and the bind-value order must stay in lockstep, the date bounds keep the
 * legacy {@code [from, from+1d)} conversion, and the dropped {@code userIds} filter stays
 * ignored.
 */
class CreditHistoryRepositoryTest {

    private final CreditHistoryRepository repository = new CreditHistoryRepository();

    @Test
    void emptySpecYieldsNoWhereClause() {
        var where = repository.buildWhere(new CreditSpecificationDTO(null, null, null, null, null, null, null));
        assertThat(where.sql()).isEmpty();
        assertThat(where.params()).isEmpty();
    }

    @Test
    void userIdsIsIgnored() {
        var where = repository.buildWhere(new CreditSpecificationDTO(
                Set.of(UUID.randomUUID()), null, null, null, null, null, null));
        assertThat(where.sql()).isEmpty();
        assertThat(where.params()).isEmpty();
    }

    @Test
    void fullSpecNumbersPlaceholdersInBindOrder() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        var where = repository.buildWhere(new CreditSpecificationDTO(
                null, from, to, 100L, 900L, RayanCreditStatus.PENDING, "Admin"));

        assertThat(where.sql()).isEqualTo(
                " WHERE c.created_at >= $1 AND c.created_at < $2 AND c.amount >= $3 AND c.amount <= $4"
                        + " AND c.status = $5 AND LOWER(c.created_by) LIKE $6");
        assertThat(where.params()).containsExactly(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                100L,
                900L,
                RayanCreditStatus.PENDING.name(),
                "%admin%");
    }
}
