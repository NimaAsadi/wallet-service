package ir.ebb.wallet.projection.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.projection.entity.TurnoverEntity;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Blocking-JDBC queries and inserts for the {@code turnover} read-model table over the shared
 * Hikari pool ({@code wallet.db}), in the {@link RayanWalletRepository} style. Services run these
 * on the blocking dispatcher; R2DBC stays exclusively under Pekko (journal + projection handler).
 *
 * <p>INSERT omits {@code created_at}/{@code updated_at} (schema defaults), and mints the
 * {@code id} when the caller left it unset — the column has no default.
 */
@Singleton
public class TurnoverRepository {

    private final DataSource dataSource;

    @Inject
    public TurnoverRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static final String INSERT = """
            INSERT INTO "turnover"(id, account_number, wallet_id, type, debit, credit, tracking_id,
                   traded_quantity, traded_price, trade_number, isin,
                   issuing_company_afc_name, instrument_afc_norm_name,
                   receipt_bank_number, withdraw_rayan_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT = """
            SELECT id, account_number, wallet_id, type, debit, credit, tracking_id,
                   traded_quantity, traded_price, trade_number, isin,
                   issuing_company_afc_name, instrument_afc_norm_name,
                   receipt_bank_number, withdraw_rayan_id, created_at, updated_at
            FROM "turnover"
            """;

    private static final String DELETE_ALL = "DELETE FROM \"turnover\"";

    private static final String FIND_ACCOUNT_NUMBER_BATCH =
            "SELECT DISTINCT account_number FROM \"turnover\" WHERE account_number > ? ORDER BY account_number ASC LIMIT ?";

    private static final String FIND_BY_ACCOUNT_NUMBER = SELECT + " WHERE account_number = ? AND type <> 'REMAINING'";

    private static final Jdbc.RowMapper<TurnoverEntity> MAPPER = TurnoverRepository::mapRow;

    public List<TurnoverEntity> findAll(TurnoverSpecificationDTO spec) {
        Where where = buildWhere(spec);
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where.sql(), MAPPER, where.params().toArray()));
    }

    public Page<TurnoverEntity> findAll(TurnoverSpecificationDTO spec, PageRequest pr) {
        Where where = buildWhere(spec);
        String paging = " ORDER BY created_at " + pr.sortDirection().name() + " LIMIT ? OFFSET ?";
        List<TurnoverEntity> content = Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, SELECT + where.sql() + paging, MAPPER,
                        appendPaging(where.params(), pr.size(), pr.offset()).toArray()));
        long total = Jdbc.withConn(dataSource, conn ->
                Jdbc.count(conn, "SELECT COUNT(*) FROM \"turnover\"" + where.sql(), where.params().toArray()));
        return new Page<>(content, total, pr.page(), pr.size());
    }

    /**
     * Next page of distinct account numbers after {@code lastAccountNumber} — the turnover-notify
     * job's cursor. A null cursor matches the legacy semantics ({@code account_number > NULL} is
     * never true → empty batch); the job seeds {@code 0L}.
     */
    public List<Long> findAccountNumberBatch(Long lastAccountNumber, int batchSize) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, FIND_ACCOUNT_NUMBER_BATCH,
                        rs -> Jdbc.getLong(rs, "account_number"),
                        lastAccountNumber, batchSize));
    }

    /** All non-REMAINING turnover rows of one account — the turnover-notify job's per-account read. */
    public List<TurnoverEntity> findByAccountNumber(Long accountNumber) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryList(conn, FIND_BY_ACCOUNT_NUMBER, MAPPER, accountNumber));
    }

    /** Full-table delete — the counterpart of the legacy bulk turnover reload. */
    public long deleteAll() {
        return Jdbc.withConn(dataSource, conn -> Jdbc.update(conn, DELETE_ALL));
    }

    /** INSERT; mints the id when unset. Returns the (possibly freshly identified) entity. */
    public TurnoverEntity save(TurnoverEntity entity) {
        if (entity.getId() == null) entity.setId(UuidCreator.getTimeOrderedEpoch());
        Jdbc.withConn(dataSource, conn -> Jdbc.update(conn, INSERT, insertParams(entity)));
        return entity;
    }

    /** Batch INSERT — one round-trip instead of N. Mints missing ids first. */
    public List<TurnoverEntity> saveAll(List<TurnoverEntity> entities) {
        List<Object[]> paramSets = new ArrayList<>(entities.size());
        for (TurnoverEntity entity : entities) {
            if (entity.getId() == null) entity.setId(UuidCreator.getTimeOrderedEpoch());
            paramSets.add(insertParams(entity));
        }
        Jdbc.withConn(dataSource, conn -> Jdbc.batchUpdate(conn, INSERT, paramSets));
        return entities;
    }

    /**
     * Package-private test seam: the WHERE text's {@code ?} numbering and the returned params
     * order must stay in lockstep. Clause set mirrors the legacy {@code buildWhere}
     * ({@code withPreBalance} was never a SQL filter and stays ignored).
     */
    Where buildWhere(TurnoverSpecificationDTO spec) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (spec.dbsAccountNumber() != null) {
            clauses.add("account_number = ?");
            params.add(spec.dbsAccountNumber());
        }
        if (spec.fromCreatedAt() != null) {
            clauses.add("created_at >= ?");
            params.add(spec.fromCreatedAt());
        }
        if (spec.toCreatedAt() != null) {
            clauses.add("created_at <= ?");
            params.add(spec.toCreatedAt().toLocalDate().atTime(23, 59, 59));
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

    private static Object[] insertParams(TurnoverEntity e) {
        return new Object[]{
                Objects.requireNonNull(e.getId(), "id"),
                Objects.requireNonNull(e.getAccountNumber(), "accountNumber"),
                Objects.requireNonNull(e.getWalletId(), "walletId"),
                e.getType(),
                e.getDebit(),
                e.getCredit(),
                e.getTrackingId(),
                e.getTradedQuantity(),
                e.getTradedPrice(),
                e.getTradeNumber(),
                e.getIsin(),
                e.getIssuingCompanyAfcName(),
                e.getInstrumentAfcNormName(),
                e.getReceiptBankNumber(),
                e.getWithdrawRayanId()
        };
    }

    private static TurnoverEntity mapRow(ResultSet rs) throws SQLException {
        TurnoverEntity e = new TurnoverEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setAccountNumber(Jdbc.getLong(rs, "account_number"));
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
    }
}
