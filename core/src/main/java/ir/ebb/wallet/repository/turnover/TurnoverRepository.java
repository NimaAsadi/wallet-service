package ir.ebb.wallet.repository.turnover;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.TurnoverEntity;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TurnoverRepository {

    private static final String SELECT = """
            SELECT id, version, user_id, account_number, wallet_id, type,
                   debit, credit, tracking_id, traded_quantity, traded_price, trade_number,
                   isin, issuing_company_afc_name, instrument_afc_norm_name,
                   receipt_bank_number, withdraw_rayan_id, created_at, updated_at
            FROM turnover
            """;

    private static final String INSERT = """
            INSERT INTO turnover (id, version, user_id, account_number, wallet_id, type,
                debit, credit, tracking_id, traded_quantity, traded_price, trade_number,
                isin, issuing_company_afc_name, instrument_afc_norm_name,
                receipt_bank_number, withdraw_rayan_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
            """;

    private final DataSource dataSource;

    private static final Jdbc.RowMapper<TurnoverEntity> MAPPER = rs -> {
        TurnoverEntity e = new TurnoverEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setVersion(rs.getLong("version"));
        e.setUser(User.of(Jdbc.getUuid(rs, "user_id"), Jdbc.getLong(rs, "account_number")));
        e.setWalletId(Jdbc.getUuid(rs, "wallet_id"));
        String type = rs.getString("type");
        e.setType(type == null ? null : TurnoverOperationType.valueOf(type));
        e.setDebit(Jdbc.getLong(rs, "debit"));
        e.setCredit(Jdbc.getLong(rs, "credit"));
        e.setTrackingId(Jdbc.getUuid(rs, "tracking_id"));
        e.setTradedQuantity(Jdbc.getLong(rs, "traded_quantity"));
        e.setTradedPrice(Jdbc.getLong(rs, "traded_price"));
        e.setTradeNumber(Jdbc.getInteger(rs, "trade_number"));
        e.setIsin(rs.getString("isin"));
        e.setIssuingCompanyAfcName(rs.getString("issuing_company_afc_name"));
        e.setInstrumentAfcNormName(rs.getString("instrument_afc_norm_name"));
        e.setReceiptBankNumber(rs.getString("receipt_bank_number"));
        e.setWithdrawRayanId(Jdbc.getLong(rs, "withdraw_rayan_id"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));
        return e;
    };

    public void deleteAll() {
        Jdbc.withConn(dataSource, conn -> Jdbc.update(conn, "DELETE FROM turnover WHERE 1=1"));
    }

    public List<Long> findAccountNumberBatch(Long lastAccountNumber, int batchSize) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn,
                        "SELECT DISTINCT account_number FROM turnover WHERE account_number > ? ORDER BY account_number ASC LIMIT ?",
                        rs -> rs.getLong(1), lastAccountNumber, batchSize));
    }

    public List<TurnoverEntity> findByAccountNumber(Long accountNumber) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + " WHERE account_number = ? AND type <> 'REMAINING'",
                        MAPPER, accountNumber));
    }

    public List<TurnoverEntity> findAll(TurnoverSpecificationDTO spec) {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(spec, params);
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where, MAPPER, params.toArray()));
    }

    public Page<TurnoverEntity> findAll(TurnoverSpecificationDTO spec, PageRequest pr) {
        List<Object> params = new ArrayList<>();
        String where = buildWhere(spec, params);
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(pr.size());
        pageParams.add(pr.offset());
        String orderBy = " ORDER BY created_at " + pr.sortDirection().name() + " LIMIT ? OFFSET ?";
        List<TurnoverEntity> content = Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where + orderBy, MAPPER, pageParams.toArray()));
        long total = Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM turnover" + where, params.toArray()));
        return new Page<>(content, total, pr.page(), pr.size());
    }

    public TurnoverEntity save(TurnoverEntity e) {
        Jdbc.inTx(dataSource, conn -> {
            insert(conn, e);
            return null;
        });
        return e;
    }

    public List<TurnoverEntity> saveAll(List<TurnoverEntity> entities) {
        Jdbc.inTx(dataSource, conn -> {
            for (TurnoverEntity e : entities) {
                insert(conn, e);
            }
            return null;
        });
        return entities;
    }

    private void insert(java.sql.Connection conn, TurnoverEntity e) {
        Jdbc.update(conn, INSERT,
                e.getId(), e.getVersion(),
                e.getUser().getKeycloakId(), e.getUser().getDbsAccountNumber(),
                e.getWalletId(), e.getType(),
                e.getDebit(), e.getCredit(), e.getTrackingId(),
                e.getTradedQuantity(), e.getTradedPrice(), e.getTradeNumber(),
                e.getIsin(), e.getIssuingCompanyAfcName(), e.getInstrumentAfcNormName(),
                e.getReceiptBankNumber(), e.getWithdrawRayanId());
    }

    private String buildWhere(TurnoverSpecificationDTO spec, List<Object> params) {
        List<String> clauses = new ArrayList<>();
        if (spec.user() != null) {
            clauses.add("user_id = ?");
            params.add(spec.user().getKeycloakId());
            clauses.add("account_number = ?");
            params.add(spec.user().getDbsAccountNumber());
        }
        if (spec.fromCreatedAt() != null) {
            clauses.add("created_at >= ?");
            params.add(spec.fromCreatedAt());
        }
        if (spec.toCreatedAt() != null) {
            clauses.add("created_at <= ?");
            params.add(spec.toCreatedAt().toLocalDate().atTime(23, 59, 59));
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }
}
