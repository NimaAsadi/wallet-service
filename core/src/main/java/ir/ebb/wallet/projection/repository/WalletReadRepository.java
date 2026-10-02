package ir.ebb.wallet.projection.repository;

import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.projection.entity.WalletEntity;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Blocking-JDBC reads for the {@code wallet} read-model table over the shared Hikari pool
 * ({@code wallet.db}). The R2DBC sibling {@link WalletRepository} serves only the projection
 * handler's session-scoped writes; every request-time read goes through here on the blocking
 * dispatcher.
 */
@Singleton
public class WalletReadRepository {

    private final DataSource dataSource;

    @Inject
    public WalletReadRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static final String SELECT = """
            SELECT id, version, account_number,
                   t0_balance, t0_frozen, t1_balance, t1_frozen, t2_balance, t2_frozen,
                   credit, initial_credit, separ_credit, separ_initial_credit,
                   created_at, updated_at
            FROM "wallet"
            """;

    private static final Jdbc.RowMapper<WalletEntity> MAPPER = WalletReadRepository::mapRow;

    public List<WalletEntity> findAll() {
        return Jdbc.withConn(dataSource, conn -> Jdbc.queryList(conn, SELECT, MAPPER));
    }

    public List<WalletEntity> findAll(WalletSpecificationDTO spec) {
        Where where = buildWhere(spec);
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where.sql(), MAPPER, where.params().toArray()));
    }

    public Page<WalletEntity> findAll(WalletSpecificationDTO spec, PageRequest pr) {
        Where where = buildWhere(spec);
        String paging = " ORDER BY created_at " + pr.sortDirection().name() + " LIMIT ? OFFSET ?";
        List<WalletEntity> content = Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where.sql() + paging, MAPPER,
                        appendPaging(where.params(), pr.size(), pr.offset()).toArray()));
        long total = Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM \"wallet\"" + where.sql(), where.params().toArray()));
        return new Page<>(content, total, pr.page(), pr.size());
    }

    public Optional<WalletEntity> findByAccountNumber(long accountNumber) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryOne(conn, SELECT + " WHERE account_number = ?", MAPPER, accountNumber));
    }

    /**
     * Legacy name kept for the service layer — the projection wallet carries no user column (the
     * legacy {@code User} embeddable was flattened away), so this is the account-number lookup.
     */
    public Optional<WalletEntity> findByUser_DbsAccountNumber(long dbsAccountNumber) {
        return findByAccountNumber(dbsAccountNumber);
    }

    /** Wallets that have consumed part of their separ credit. */
    public List<WalletEntity> findSeparCreditDebtors() {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + " WHERE separ_credit < separ_initial_credit", MAPPER));
    }

    /** Whether one account has consumed part of its separ credit. */
    public boolean existsByUserAndSeparCreditLessThanSeparInitialCredit(long accountNumber) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM \"wallet\" WHERE account_number = ? AND separ_credit < separ_initial_credit",
                        accountNumber) > 0);
    }

    /**
     * Package-private test seam: the WHERE text's {@code ?} numbering and the returned params
     * order must stay in lockstep. {@code userId}/{@code userIds} are deliberately ignored — the
     * projection wallet table has no user columns (flattened away in {@code WalletEntity}).
     */
    Where buildWhere(WalletSpecificationDTO spec) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (spec.dbsAccountNumber() != null) {
            clauses.add("account_number = ?");
            params.add(spec.dbsAccountNumber());
        }
        if (spec.accountNumbers() != null && !spec.accountNumbers().isEmpty()) {
            clauses.add("account_number IN (" + placeholders(spec.accountNumbers().size()) + ")");
            params.addAll(spec.accountNumbers());
        }
        range(clauses, params, "t0_balance", spec.fromT0Balance(), spec.toT0Balance());
        range(clauses, params, "t1_balance", spec.fromT1Balance(), spec.toT1Balance());
        range(clauses, params, "t2_balance", spec.fromT2Balance(), spec.toT2Balance());
        range(clauses, params, "initial_credit", spec.fromInitialCredit(), spec.toInitialCredit());
        range(clauses, params, "credit", spec.fromCredit(), spec.toCredit());
        // No single frozen column — filter on the sum of the three tiers.
        if (spec.fromFrozen() != null || spec.toFrozen() != null)
            range(clauses, params, "(t0_frozen + t1_frozen + t2_frozen)", spec.fromFrozen(), spec.toFrozen());
        if (Boolean.TRUE.equals(spec.separCreditDebtor()))
            clauses.add("separ_credit < separ_initial_credit");
        else if (Boolean.FALSE.equals(spec.separCreditDebtor()))
            clauses.add("separ_credit >= separ_initial_credit");
        return clauses.isEmpty() ? new Where("", params) : new Where(" WHERE " + String.join(" AND ", clauses), params);
    }

    /** WHERE fragment plus its bind values, in bind order. Package-private for tests. */
    record Where(String sql, List<Object> params) {
    }

    private static void range(List<String> clauses, List<Object> params, String column, Long from, Long to) {
        if (from != null) {
            clauses.add(column + " >= ?");
            params.add(from);
        }
        if (to != null) {
            clauses.add(column + " <= ?");
            params.add(to);
        }
    }

    private static String placeholders(int count) {
        return IntStream.range(0, count).mapToObj(i -> "?").collect(Collectors.joining(", "));
    }

    private static List<Object> appendPaging(List<Object> params, int size, long offset) {
        List<Object> all = new ArrayList<>(params);
        all.add(size);
        all.add(offset);
        return all;
    }

    /** All numeric columns except {@code id} are primitive fields on the entity — 0 on SQL NULL. */
    private static WalletEntity mapRow(ResultSet rs) throws SQLException {
        WalletEntity e = new WalletEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setVersion(rs.getLong("version"));
        e.setAccountNumber(rs.getLong("account_number"));
        e.setT0Balance(rs.getLong("t0_balance"));
        e.setT0Frozen(rs.getLong("t0_frozen"));
        e.setT1Balance(rs.getLong("t1_balance"));
        e.setT1Frozen(rs.getLong("t1_frozen"));
        e.setT2Balance(rs.getLong("t2_balance"));
        e.setT2Frozen(rs.getLong("t2_frozen"));
        e.setCredit(rs.getLong("credit"));
        e.setInitialCredit(rs.getLong("initial_credit"));
        e.setSeparCredit(rs.getLong("separ_credit"));
        e.setSeparInitialCredit(rs.getLong("separ_initial_credit"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));
        return e;
    }
}
