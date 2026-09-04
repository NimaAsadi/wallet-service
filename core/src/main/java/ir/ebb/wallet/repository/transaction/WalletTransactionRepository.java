package ir.ebb.wallet.repository.transaction;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.entity.WalletTransactionEntity;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WalletTransactionRepository {

    private static final String SELECT = """
            SELECT id, version, user_id, account_number, wallet_id,
                   wallet_operation_type, wallet_transaction_type, wallet_parameter_type,
                   amount, tracking_id, frozen_before, frozen_after, balance_before, balance_after,
                   created_at, updated_at
            FROM wallet_transaction
            """;

    private static final String INSERT = """
            INSERT INTO wallet_transaction (id, version, account_number, wallet_id,
                wallet_operation_type, wallet_transaction_type, wallet_parameter_type,
                amount, tracking_id, frozen_before, frozen_after, balance_before, balance_after,
                created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
            """;

    private final DataSource dataSource;

    private static final Jdbc.RowMapper<WalletTransactionEntity> MAPPER = rs -> {
        WalletTransactionEntity e = new WalletTransactionEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setVersion(rs.getLong("version"));
        e.setAccountNumber(Jdbc.getLong(rs, "account_number"));
        e.setWalletId(Jdbc.getUuid(rs, "wallet_id"));
        e.setWalletOperationType(enumOrNull(rs.getString("wallet_operation_type"),
                ir.ebb.wallet.constant.enumeration.WalletOperationType.class));
        e.setWalletTransactionType(enumOrNull(rs.getString("wallet_transaction_type"),
                ir.ebb.wallet.constant.enumeration.WalletTransactionType.class));
        e.setWalletParameterType(enumOrNull(rs.getString("wallet_parameter_type"),
                ir.ebb.wallet.constant.enumeration.WalletParameterType.class));
        e.setAmount(Jdbc.getLong(rs, "amount"));
        e.setTrackingId(Jdbc.getUuid(rs, "tracking_id"));
        e.setFrozenBefore(Jdbc.getLong(rs, "frozen_before"));
        e.setFrozenAfter(Jdbc.getLong(rs, "frozen_after"));
        e.setBalanceBefore(Jdbc.getLong(rs, "balance_before"));
        e.setBalanceAfter(Jdbc.getLong(rs, "balance_after"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));
        return e;
    };

    public Page<WalletTransactionEntity> findAll(WalletTransactionSpecificationDTO spec, PageRequest pr) {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(spec, params);
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pr.size());
        pageParams.add(pr.offset());
        String orderBy = " ORDER BY " + ("createdAt".equals(pr.sortProperty()) ? "created_at" : "created_at")
                + " " + pr.sortDirection().name() + " LIMIT ? OFFSET ?";
        List<WalletTransactionEntity> content = Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where + orderBy, MAPPER, pageParams.toArray()));
        long total = Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM wallet_transaction" + where, params.toArray()));
        return new Page<>(content, total, pr.page(), pr.size());
    }

    /** Inserts all transactions on the caller's connection (part of a wallet mutation tx). */
    public void saveAll(Connection conn, List<WalletTransactionEntity> txs) {
        for (WalletTransactionEntity e : txs) {
            Jdbc.update(conn, INSERT,
                    e.getId(), e.getVersion(),
                    e.getAccountNumber(),
                    e.getWalletId(),
                    e.getWalletOperationType(), e.getWalletTransactionType(), e.getWalletParameterType(),
                    e.getAmount(), e.getTrackingId(),
                    e.getFrozenBefore(), e.getFrozenAfter(),
                    e.getBalanceBefore(), e.getBalanceAfter());
        }
    }

    private String buildWhere(WalletTransactionSpecificationDTO spec, List<Object> params) {
        List<String> clauses = new ArrayList<>();
        if (spec.userId() != null && !spec.userId().isBlank()) {
            clauses.add("user_id = ?");
            params.add(UUID.fromString(spec.userId()));
        }
        if (spec.dbsAccountNumber() != null) {
            clauses.add("account_number = ?");
            params.add(spec.dbsAccountNumber());
        }
        if (spec.type() != null) {
            clauses.add("wallet_transaction_type = ?");
            params.add(spec.type());
        }
        if (spec.trackingCode() != null) {
            clauses.add("tracking_id = ?");
            params.add(spec.trackingCode());
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private static <E extends Enum<E>> E enumOrNull(String value, Class<E> type) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
