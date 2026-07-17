package ir.ebb.wallet.repository.credit;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.common.model.user.User;
import ir.ebb.userinfo.entity.UserEntity;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CreditHistoryRepository {

    private static final String SELECT = """
            SELECT c.id, c.version, c.user_id, c.account_number, c.amount, c.status,
                   c.created_id, c.created_by, c.error_message, c.created_at, c.updated_at,
                   u.national_code, u.full_name, u.account_name, u.father_name
            FROM credit_history c
            LEFT JOIN user_info u ON u.user_id = c.user_id AND u.account_number = c.account_number
            """;

    private static final String INSERT = """
            INSERT INTO credit_history (id, version, user_id, account_number, amount, status,
                created_id, created_by, error_message, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
            """;

    private final DataSource dataSource;

    private static final Jdbc.RowMapper<CreditHistoryEntity> MAPPER = rs -> {
        UserEntity user = new UserEntity();
        user.setUser(User.of(Jdbc.getUuid(rs, "user_id"), Jdbc.getLong(rs, "account_number")));
        user.setNationalCode(rs.getString("national_code"));
        user.setFullName(rs.getString("full_name"));
        user.setAccountName(rs.getString("account_name"));
        user.setFatherName(rs.getString("father_name"));

        CreditHistoryEntity e = new CreditHistoryEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setVersion(rs.getLong("version"));
        e.setUser(user);
        e.setAmount(Jdbc.getLong(rs, "amount"));
        e.setStatus(rs.getString("status") == null ? null : RayanCreditStatus.valueOf(rs.getString("status")));
        e.setCreatedId(Jdbc.getUuid(rs, "created_id"));
        e.setCreatedBy(rs.getString("created_by"));
        e.setErrorMessage(rs.getString("error_message"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));
        return e;
    };

    public Page<CreditHistoryEntity> findAll(CreditSpecificationDTO spec, PageRequest pr) {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(spec, params);
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pr.size());
        pageParams.add(pr.offset());
        String orderBy = " ORDER BY c.created_at " + pr.sortDirection().name() + " LIMIT ? OFFSET ?";
        List<CreditHistoryEntity> content = Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where + orderBy, MAPPER, pageParams.toArray()));
        long total = Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM credit_history c" + where, params.toArray()));
        return new Page<>(content, total, pr.page(), pr.size());
    }

    /** Inserts a credit-history row on the caller's connection (part of saveForCredit tx). */
    public void save(Connection conn, CreditHistoryEntity e) {
        Jdbc.update(conn, INSERT,
                e.getId(), e.getVersion(),
                e.getUser().getUser().getKeycloakId(), e.getUser().getUser().getDbsAccountNumber(),
                e.getAmount(), e.getStatus(),
                e.getCreatedId(), e.getCreatedBy(), e.getErrorMessage());
    }

    /**
     * Inserts a credit-history audit row on its own connection. Credit history is an audit record
     * (it carries admin metadata — createdId/createdBy/errorMessage — that is not part of the wallet
     * event stream), so it is written directly alongside the entity's {@code AddCredit} command
     * rather than being event-sourced.
     */
    public void save(CreditHistoryEntity e) {
        Jdbc.withConn(dataSource, conn -> {
            save(conn, e);
            return null;
        });
    }

    private String buildWhere(CreditSpecificationDTO spec, List<Object> params) {
        List<String> clauses = new ArrayList<>();
        if (spec.userIds() != null && !spec.userIds().isEmpty()) {
            clauses.add("c.user_id IN (" + placeholders(spec.userIds().size()) + ")");
            params.addAll(spec.userIds());
        }
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
            params.add(spec.status());
        }
        if (spec.createdBy() != null && !spec.createdBy().isBlank()) {
            clauses.add("LOWER(c.created_by) LIKE ?");
            params.add("%" + spec.createdBy().toLowerCase() + "%");
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }
}
