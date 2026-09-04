package ir.ebb.wallet.repository;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.common.model.user.User;
import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.entity.WalletDebtEntity;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.entity.WalletParameterEmbedded;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-model repository for the {@code wallet}/{@code wallet_debt} projection tables.
 * <b>Read-only</b>: the wallet aggregate is event-sourced (the {@code WalletEntity}); these
 * tables are populated by the wallet projection and are currently FROZEN — the old projection
 * was removed pending the new projection for the next-gen wallet actor. All reads are
 * self-contained (own connection from the pool).
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WalletRepository {

    private static final String SELECT_WALLET = """
            SELECT w.id, w.version, w.user_id, w.account_number,
                   w.t0_balance, w.t0_frozen, w.t1_balance, w.t1_frozen, w.t2_balance, w.t2_frozen,
                   w.credit, w.initial_credit, w.separ_credit, w.separ_initial_credit,
                   w.created_at, w.updated_at,
                   d.t2_to_t0_debt, d.t2_to_t1_debt, d.t1_to_t0_debt
            FROM wallet w LEFT JOIN wallet_debt d ON d.wallet_id = w.id
            """;

    private final DataSource dataSource;

    private static final Jdbc.RowMapper<WalletEntity> WALLET_MAPPER = rs -> {
        WalletEntity e = new WalletEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setVersion(rs.getLong("version"));
        e.setAccountNumber(Jdbc.getLong(rs, "account_number"));
        e.setT0(new WalletParameterEmbedded(Jdbc.getLong(rs, "t0_balance"), Jdbc.getLong(rs, "t0_frozen")));
        e.setT1(new WalletParameterEmbedded(Jdbc.getLong(rs, "t1_balance"), Jdbc.getLong(rs, "t1_frozen")));
        e.setT2(new WalletParameterEmbedded(Jdbc.getLong(rs, "t2_balance"), Jdbc.getLong(rs, "t2_frozen")));
        e.setCredit(Jdbc.getLong(rs, "credit"));
        e.setInitialCredit(Jdbc.getLong(rs, "initial_credit"));
        e.setSeparCredit(Jdbc.getLong(rs, "separ_credit"));
        e.setSeparInitialCredit(Jdbc.getLong(rs, "separ_initial_credit"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));

        WalletDebtEntity debt = new WalletDebtEntity();
        debt.setId(e.getId());
        debt.setT2Tot0Debt(Jdbc.getLong(rs, "t2_to_t0_debt"));
        debt.setT2Tot1Debt(Jdbc.getLong(rs, "t2_to_t1_debt"));
        debt.setT1Tot0Debt(Jdbc.getLong(rs, "t1_to_t0_debt"));
        e.setWalletDebtEntity(debt);
        return e;
    };

    // ── reads ─────────────────────────────────────────────────────────────────

    public Optional<WalletEntity> findFirstByUser(User user) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryOne(conn, SELECT_WALLET + " WHERE w.user_id = ? AND w.account_number = ? LIMIT 1",
                        WALLET_MAPPER, user.getKeycloakId(), user.getDbsAccountNumber()));
    }

    public Optional<WalletEntity> findByUser_DbsAccountNumber(long dbsAccountNumber) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryOne(conn, SELECT_WALLET + " WHERE w.account_number = ?",
                        WALLET_MAPPER, dbsAccountNumber));
    }

    public boolean existsByUserAndSeparCreditLessThanSeparInitialCredit(User user) {
        String sql = "SELECT EXISTS (SELECT 1 FROM wallet WHERE user_id = ? AND account_number = ? " +
                     "AND separ_credit < separ_initial_credit)";
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryOne(conn, sql, rs -> rs.getBoolean(1), user.getKeycloakId(), user.getDbsAccountNumber())
                        .orElse(false));
    }

    public List<WalletEntity> findSeparCreditDebtors() {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT_WALLET + " WHERE w.separ_initial_credit > 0 OR w.separ_credit < w.separ_initial_credit",
                        WALLET_MAPPER));
    }

    public List<WalletEntity> findAll() {
        return Jdbc.withConn(dataSource, conn -> Jdbc.queryList(conn, SELECT_WALLET, WALLET_MAPPER));
    }

    public List<WalletEntity> findAll(WalletSpecificationDTO spec) {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(spec, params);
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT_WALLET + where, WALLET_MAPPER, params.toArray()));
    }

    public Page<WalletEntity> findAll(WalletSpecificationDTO spec, PageRequest pr) {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(spec, params);
        String orderBy = " ORDER BY " + sortColumn(pr.sortProperty()) + " " + pr.sortDirection().name()
                + " LIMIT ? OFFSET ?";
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pr.size());
        pageParams.add(pr.offset());
        List<WalletEntity> content = Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT_WALLET + where + orderBy, WALLET_MAPPER, pageParams.toArray()));
        long total = Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM wallet w" + where, params.toArray()));
        return new Page<>(content, total, pr.page(), pr.size());
    }

    // ── spec → WHERE builder (mirrors the former WalletSpecification) ──────────

    private String buildWhere(WalletSpecificationDTO spec, List<Object> params) {
        List<String> clauses = new ArrayList<>();
        if (spec.userId() != null) {
            clauses.add("w.user_id = ?");
            params.add(spec.userId());
        }
        if (spec.dbsAccountNumber() != null) {
            clauses.add("w.account_number = ?");
            params.add(spec.dbsAccountNumber());
        }
        if (spec.userIds() != null && !spec.userIds().isEmpty()) {
            clauses.add("w.user_id IN (" + placeholders(spec.userIds().size()) + ")");
            params.addAll(spec.userIds());
        }
        if (spec.accountNumbers() != null && !spec.accountNumbers().isEmpty()) {
            clauses.add("w.account_number IN (" + placeholders(spec.accountNumbers().size()) + ")");
            params.addAll(spec.accountNumbers());
        }
        addRange(clauses, params, "w.t0_balance", spec.fromT0Balance(), spec.toT0Balance());
        addRange(clauses, params, "w.t1_balance", spec.fromT1Balance(), spec.toT1Balance());
        addRange(clauses, params, "w.t2_balance", spec.fromT2Balance(), spec.toT2Balance());
        addRange(clauses, params, "w.initial_credit", spec.fromInitialCredit(), spec.toInitialCredit());
        addRange(clauses, params, "w.credit", spec.fromCredit(), spec.toCredit());
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private void addRange(List<String> clauses, List<Object> params, String column, Long from, Long to) {
        if (from != null) {
            clauses.add(column + " >= ?");
            params.add(from);
        }
        if (to != null) {
            clauses.add(column + " <= ?");
            params.add(to);
        }
    }

    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    private static String sortColumn(String property) {
        return switch (property == null ? "createdAt" : property) {
            case "createdAt" -> "w.created_at";
            case "accountNumber", "dbsAccountNumber" -> "w.account_number";
            case "credit" -> "w.credit";
            case "initialCredit" -> "w.initial_credit";
            default -> "w.created_at";
        };
    }
}
