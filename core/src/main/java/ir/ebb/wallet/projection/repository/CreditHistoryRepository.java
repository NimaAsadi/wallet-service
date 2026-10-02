package ir.ebb.wallet.projection.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.projection.entity.CreditHistoryEntity;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Blocking-JDBC queries and inserts for the {@code credit_history} read-model table over the
 * shared Hikari pool ({@code wallet.db}) — admin credit-op audit rows, written outside the wallet
 * events. Services run these on the blocking dispatcher; R2DBC stays exclusively under Pekko.
 *
 * <p>The legacy SELECT LEFT-JOINed {@code user_info} to feed denormalized user fields; the
 * projection entity has no user columns and the legacy {@code userIds} filter targeted a
 * {@code user_id} column the table no longer carries — both are dropped, and {@link #buildWhere}
 * ignores {@code spec.userIds()}.
 */
@Singleton
public class CreditHistoryRepository {

    private final DataSource dataSource;

    @Inject
    public CreditHistoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static final String INSERT = """
            INSERT INTO "credit_history"(id, account_number, amount, status, created_id, created_by, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT = """
            SELECT c.id, c.account_number, c.amount, c.status, c.created_id, c.created_by,
                   c.error_message, c.created_at, c.updated_at
            FROM "credit_history" c
            """;

    private static final Jdbc.RowMapper<CreditHistoryEntity> MAPPER = CreditHistoryRepository::mapRow;

    public Page<CreditHistoryEntity> findAll(CreditSpecificationDTO spec, PageRequest pr) {
        Where where = buildWhere(spec);
        String paging = " ORDER BY c.created_at " + pr.sortDirection().name() + " LIMIT ? OFFSET ?";
        List<CreditHistoryEntity> content = Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where.sql() + paging, MAPPER,
                        appendPaging(where.params(), pr.size(), pr.offset()).toArray()));
        long total = Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM \"credit_history\" c" + where.sql(), where.params().toArray()));
        return new Page<>(content, total, pr.page(), pr.size());
    }

    /** INSERT; mints the id when unset. Returns the (possibly freshly identified) entity. */
    public CreditHistoryEntity save(CreditHistoryEntity entity) {
        if (entity.getId() == null) entity.setId(UuidCreator.getTimeOrderedEpoch());
        Jdbc.withConn(dataSource, conn -> Jdbc.update(conn, INSERT,
                entity.getId(),
                entity.getAccountNumber(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedId(),
                entity.getCreatedBy(),
                entity.getErrorMessage()));
        return entity;
    }

    /**
     * Package-private test seam: the WHERE text's {@code ?} numbering and the returned params
     * order must stay in lockstep. {@code status} binds as its name (the table stores the enum as
     * text); {@code userIds} is deliberately ignored (no user column).
     */
    Where buildWhere(CreditSpecificationDTO spec) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (spec.fromDate() != null) {
            clauses.add("c.created_at >= ?");
            params.add(spec.fromDate().atStartOfDay());
        }
        if (spec.toDate() != null) {
            clauses.add("c.created_at < ?");
            params.add(spec.toDate().plusDays(1).atStartOfDay());
        }
        if (spec.fromAmount() != null) {
            clauses.add("c.amount >= ?");
            params.add(spec.fromAmount());
        }
        if (spec.toAmount() != null) {
            clauses.add("c.amount <= ?");
            params.add(spec.toAmount());
        }
        if (spec.status() != null) {
            clauses.add("c.status = ?");
            params.add(spec.status().name());
        }
        if (spec.createdBy() != null && !spec.createdBy().isBlank()) {
            clauses.add("LOWER(c.created_by) LIKE ?");
            params.add("%" + spec.createdBy().toLowerCase() + "%");
        }
        return clauses.isEmpty() ? new Where("", params) : new Where(" WHERE " + String.join(" AND ", clauses), params);
    }

    /** WHERE fragment plus its bind values, in bind order. Package-private for tests. */
    record Where(String sql, List<Object> params) {
    }

    private static List<Object> appendPaging(List<Object> params, int size, long offset) {
        List<Object> all = new ArrayList<>(params);
        all.add(size);
        all.add(offset);
        return all;
    }

    private static CreditHistoryEntity mapRow(ResultSet rs) throws SQLException {
        CreditHistoryEntity e = new CreditHistoryEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setAccountNumber(Jdbc.getLong(rs, "account_number"));
        e.setAmount(Jdbc.getLong(rs, "amount"));
        String status = rs.getString("status");
        e.setStatus(status == null ? null : RayanCreditStatus.valueOf(status));
        e.setCreatedId(Jdbc.getUuid(rs, "created_id"));
        e.setCreatedBy(rs.getString("created_by"));
        e.setErrorMessage(rs.getString("error_message"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));
        return e;
    }
}
