package ir.ebb.wallet.projection.repository;

import io.r2dbc.spi.Statement;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.projection.entity.CreditHistoryEntity;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Projection-side queries for the {@code credit_history} read-model table — the R2DBC
 * counterpart of the legacy {@code ir.ebb.wallet.repository.credit.CreditHistoryRepository}
 * (blocking JDBC).
 */
@Singleton
public class CreditHistoryRepository extends BaseCreditHistoryRepository {

    /** Explicit for Dagger — the inherited default ctor is invisible to annotation processing. */
    @Inject
    public CreditHistoryRepository() {
    }

    /**
     * Explicit column list with the {@code c} alias — not {@code SELECT *} — so the labels stay
     * stable for the generated {@link #mapRow}. The legacy SELECT also LEFT-JOINed
     * {@code user_info} to feed denormalized user fields; the projection entity has no user
     * columns (the legacy mapper feeding them is a commented-out stub), and the legacy
     * {@code userIds} filter targeted a {@code user_id} column the table no longer carries —
     * both are dropped, and {@link #buildWhere} ignores {@code spec.userIds()}.
     */
    private static final String SELECT = """
            SELECT c.id, c.account_number, c.amount, c.status, c.created_id, c.created_by,
                   c.error_message, c.created_at, c.updated_at
            FROM "credit_history" c
            """;

    public CompletionStage<Page<CreditHistoryEntity>> findAll(R2dbcSession session, CreditSpecificationDTO spec, PageRequest pr) {
        Where where = buildWhere(spec);
        int paramCount = where.params().size();
        String paging = " ORDER BY c.created_at " + pr.sortDirection().name()
                + " LIMIT $" + (paramCount + 1) + " OFFSET $" + (paramCount + 2);
        Statement content = session.createStatement(SELECT + where.sql() + paging);
        bindParams(content, where.params());
        content.bind(paramCount, pr.size());
        content.bind(paramCount + 1, pr.offset());

        Statement count = session.createStatement("SELECT COUNT(*) FROM \"credit_history\" c" + where.sql());
        bindParams(count, where.params());

        return session.select(content, this::mapRow)
                .thenCombine(session.selectOne(count, row -> row.get(0, Long.class)),
                        (rows, total) -> new Page<>(rows, total.orElse(0L), pr.page(), pr.size()));
    }

    /**
     * Package-private test seam: the WHERE text's {@code $n} numbering and the returned params
     * order must stay in lockstep with {@link #bindParams}. Clause set mirrors the legacy
     * {@code buildWhere} minus the dropped {@code userIds} filter; {@code status} binds as its
     * name (the table stores the enum as text, like the generated INSERT).
     */
    Where buildWhere(CreditSpecificationDTO spec) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (spec.fromDate() != null) {
            clauses.add("c.created_at >= $" + (params.size() + 1));
            params.add(spec.fromDate().atStartOfDay());
        }
        if (spec.toDate() != null) {
            clauses.add("c.created_at < $" + (params.size() + 1));
            params.add(spec.toDate().plusDays(1).atStartOfDay());
        }
        if (spec.fromAmount() != null) {
            clauses.add("c.amount >= $" + (params.size() + 1));
            params.add(spec.fromAmount());
        }
        if (spec.toAmount() != null) {
            clauses.add("c.amount <= $" + (params.size() + 1));
            params.add(spec.toAmount());
        }
        if (spec.status() != null) {
            clauses.add("c.status = $" + (params.size() + 1));
            params.add(spec.status().name());
        }
        if (spec.createdBy() != null && !spec.createdBy().isBlank()) {
            clauses.add("LOWER(c.created_by) LIKE $" + (params.size() + 1));
            params.add("%" + spec.createdBy().toLowerCase() + "%");
        }
        return clauses.isEmpty() ? new Where("", params) : new Where(" WHERE " + String.join(" AND ", clauses), params);
    }

    /** WHERE fragment plus its bind values, in bind order. Package-private for tests. */
    record Where(String sql, List<Object> params) {
    }

    private static void bindParams(Statement statement, List<Object> params) {
        for (int i = 0; i < params.size(); i++) {
            statement.bind(i, params.get(i));
        }
    }
}
