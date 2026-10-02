package ir.ebb.wallet.projection.repository;

import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.projection.entity.WalletTransactionEntity;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Blocking-JDBC reads for the {@code wallet_transaction} read-model table over the shared Hikari
 * pool ({@code wallet.db}). The R2DBC sibling {@link WalletTransactionRepository} serves only the
 * projection handler's leg inserts.
 */
@Singleton
public class WalletTransactionReadRepository {

    private final DataSource dataSource;

    @Inject
    public WalletTransactionReadRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static final String SELECT = """
            SELECT id, version, account_number, wallet_id, wallet_operation_type,
                   wallet_transaction_type, wallet_parameter_type, amount, tracking_id,
                   frozen_before, frozen_after, balance_before, balance_after,
                   created_at, updated_at
            FROM "wallet_transaction"
            """;

    private static final Jdbc.RowMapper<WalletTransactionEntity> MAPPER = WalletTransactionReadRepository::mapRow;

    public Page<WalletTransactionEntity> findAll(WalletTransactionSpecificationDTO spec, PageRequest pr) {
        Where where = buildWhere(spec);
        String paging = " ORDER BY created_at " + pr.sortDirection().name() + " LIMIT ? OFFSET ?";
        List<WalletTransactionEntity> content = Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where.sql() + paging, MAPPER,
                        appendPaging(where.params(), pr.size(), pr.offset()).toArray()));
        long total = Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM \"wallet_transaction\"" + where.sql(), where.params().toArray()));
        return new Page<>(content, total, pr.page(), pr.size());
    }

    /**
     * Package-private test seam: the WHERE text's {@code ?} numbering and the returned params
     * order must stay in lockstep. {@code userId} is deliberately ignored — the projection
     * wallet_transaction table has no user column.
     */
    Where buildWhere(WalletTransactionSpecificationDTO spec) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (spec.dbsAccountNumber() != null) {
            clauses.add("account_number = ?");
            params.add(spec.dbsAccountNumber());
        }
        if (spec.type() != null) {
            clauses.add("wallet_transaction_type = ?");
            params.add(spec.type().name());
        }
        if (spec.trackingCode() != null) {
            clauses.add("tracking_id = ?");
            params.add(spec.trackingCode());
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

    private static WalletTransactionEntity mapRow(ResultSet rs) throws SQLException {
        WalletTransactionEntity e = new WalletTransactionEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setVersion(rs.getLong("version"));
        e.setAccountNumber(rs.getLong("account_number"));
        e.setWalletId(Jdbc.getUuid(rs, "wallet_id"));
        e.setWalletOperationType(enumOf(rs, "wallet_operation_type", WalletOperationType.class));
        e.setWalletTransactionType(enumOf(rs, "wallet_transaction_type", WalletTransactionType.class));
        e.setWalletParameterType(enumOf(rs, "wallet_parameter_type", WalletParameterType.class));
        e.setAmount(rs.getLong("amount"));
        e.setTrackingId(Jdbc.getUuid(rs, "tracking_id"));
        e.setFrozenBefore(Jdbc.getLong(rs, "frozen_before"));
        e.setFrozenAfter(Jdbc.getLong(rs, "frozen_after"));
        e.setBalanceBefore(Jdbc.getLong(rs, "balance_before"));
        e.setBalanceAfter(Jdbc.getLong(rs, "balance_after"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));
        return e;
    }

    private static <E extends Enum<E>> E enumOf(ResultSet rs, String column, Class<E> type) throws SQLException {
        String name = rs.getString(column);
        return name == null ? null : Enum.valueOf(type, name);
    }
}
