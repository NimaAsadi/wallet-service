package ir.ebb.wallet.projection.repository;

import io.r2dbc.spi.Statement;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.projection.entity.TurnoverEntity;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Projection-side queries for the {@code turnover} read-model table — the R2DBC counterpart of
 * the legacy {@code ir.ebb.wallet.repository.turnover.TurnoverRepository} (blocking JDBC).
 */
@Singleton
public class TurnoverRepository extends BaseTurnoverRepository {

    /** Explicit for Dagger — the inherited default ctor is invisible to annotation processing. */
    @Inject
    public TurnoverRepository() {
    }

    /** Copied verbatim from the generated repository (its constant is private). */
    private static final String INSERT_STATEMENT = "INSERT INTO \"turnover\"(id, account_number, wallet_id, type, debit, credit, tracking_id, traded_quantity, traded_price, trade_number, isin, issuing_company_afc_name, instrument_afc_norm_name, receipt_bank_number, withdraw_rayan_id) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)";

    /**
     * Explicit column list — not {@code SELECT *} — so the labels stay stable for the generated
     * {@link #mapRow}. The legacy SELECT also carried {@code version}, which the projection
     * entity does not map; it is dropped.
     */
    private static final String SELECT = """
            SELECT id, account_number, wallet_id, type, debit, credit, tracking_id,
                   traded_quantity, traded_price, trade_number, isin,
                   issuing_company_afc_name, instrument_afc_norm_name,
                   receipt_bank_number, withdraw_rayan_id, created_at, updated_at
            FROM "turnover"
            """;

    private static final String DELETE_ALL_STATEMENT = "DELETE FROM \"turnover\"";

    private static final String FIND_ACCOUNT_NUMBER_BATCH_STATEMENT =
            "SELECT DISTINCT account_number FROM \"turnover\" WHERE account_number > $1 ORDER BY account_number ASC LIMIT $2";

    private static final String FIND_BY_ACCOUNT_NUMBER_STATEMENT =
            SELECT + " WHERE account_number = $1 AND type <> 'REMAINING'";

    /** Full-table delete — the projection-side counterpart of the legacy bulk turnover reload. */
    public CompletionStage<Long> deleteAll(R2dbcSession session) {
        return session.updateOne(session.createStatement(DELETE_ALL_STATEMENT));
    }

    /** Batch insert for the bulk reload — one statement per entity in a single batch call. */
    public CompletionStage<List<Long>> saveAll(R2dbcSession session, List<TurnoverEntity> entities) {
        return session.update(entities.stream().map(e -> saveStatement(session, e)).toList());
    }

    /**
     * Next page of distinct account numbers after {@code lastAccountNumber} — the
     * turnover-notify job's cursor. A null cursor matches the legacy JDBC null-bind semantics
     * ({@code account_number > NULL} is never true → empty batch); the job seeds {@code 0L}.
     */
    public CompletionStage<List<Long>> findAccountNumberBatch(R2dbcSession session, Long lastAccountNumber, int batchSize) {
        Statement statement = session.createStatement(FIND_ACCOUNT_NUMBER_BATCH_STATEMENT);
        bindNullable(statement, 0, lastAccountNumber);
        statement.bind(1, batchSize);
        return session.select(statement, row -> row.get(0, Long.class));
    }

    /** All non-REMAINING turnover rows of one account — the turnover-notify job's per-account read. */
    public CompletionStage<List<TurnoverEntity>> findByAccountNumber(R2dbcSession session, Long accountNumber) {
        Statement statement = session.createStatement(FIND_BY_ACCOUNT_NUMBER_STATEMENT).bind(0, accountNumber);
        return session.select(statement, this::mapRow);
    }

    public CompletionStage<List<TurnoverEntity>> findAll(R2dbcSession session, TurnoverSpecificationDTO spec) {
        Where where = buildWhere(spec);
        Statement statement = session.createStatement(SELECT + where.sql());
        bindParams(statement, where.params());
        return session.select(statement, this::mapRow);
    }

    public CompletionStage<Page<TurnoverEntity>> findAll(R2dbcSession session, TurnoverSpecificationDTO spec, PageRequest pr) {
        Where where = buildWhere(spec);
        int paramCount = where.params().size();
        String paging = " ORDER BY created_at " + pr.sortDirection().name()
                + " LIMIT $" + (paramCount + 1) + " OFFSET $" + (paramCount + 2);
        Statement content = session.createStatement(SELECT + where.sql() + paging);
        bindParams(content, where.params());
        content.bind(paramCount, pr.size());
        content.bind(paramCount + 1, pr.offset());

        Statement count = session.createStatement("SELECT COUNT(*) FROM \"turnover\"" + where.sql());
        bindParams(count, where.params());

        return session.select(content, this::mapRow)
                .thenCombine(session.selectOne(count, row -> row.get(0, Long.class)),
                        (rows, total) -> new Page<>(rows, total.orElse(0L), pr.page(), pr.size()));
    }

    /**
     * Package-private test seam: the WHERE text's {@code $n} numbering and the returned params
     * order must stay in lockstep with {@link #bindParams}. Clause set mirrors the legacy
     * {@code buildWhere} ({@code withPreBalance} was never a SQL filter and stays ignored).
     */
    Where buildWhere(TurnoverSpecificationDTO spec) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (spec.dbsAccountNumber() != null) {
            clauses.add("account_number = $" + (params.size() + 1));
            params.add(spec.dbsAccountNumber());
        }
        if (spec.fromCreatedAt() != null) {
            clauses.add("created_at >= $" + (params.size() + 1));
            params.add(spec.fromCreatedAt());
        }
        if (spec.toCreatedAt() != null) {
            clauses.add("created_at <= $" + (params.size() + 1));
            params.add(spec.toCreatedAt().toLocalDate().atTime(23, 59, 59));
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

    /**
     * Null-safe binding for the {@code turnover} INSERT. The generated {@code saveStatement}
     * binds every column directly, but the Postgres R2DBC driver rejects null binds — and real
     * rows carry nulls: the entity's convenience constructors leave {@code trackingId},
     * {@code tradeNumber}, the free-text columns and {@code withdrawRayanId} unset. Overriding
     * {@code saveStatement} covers every write path ({@code insertStatement} delegates to it);
     * identity/enum columns stay guarded with {@code requireNonNull} (a row without them is a
     * programming error, not a schema null), and the four quantity/amount columns bind directly
     * (the entity's field initializers default them to {@code 0}).
     */
    @Override
    public Statement saveStatement(R2dbcSession session, TurnoverEntity entity) {
        Statement statement = session.createStatement(INSERT_STATEMENT)
                .bind(0, Objects.requireNonNull(entity.getId(), "id"))
                .bind(1, Objects.requireNonNull(entity.getAccountNumber(), "accountNumber"))
                .bind(2, Objects.requireNonNull(entity.getWalletId(), "walletId"))
                .bind(3, Objects.requireNonNull(entity.getType(), "type").name())
                .bind(4, entity.getDebit())
                .bind(5, entity.getCredit());
        bindNullable(statement, 6, entity.getTrackingId());
        statement.bind(7, entity.getTradedQuantity());
        statement.bind(8, entity.getTradedPrice());
        bindNullable(statement, 9, entity.getTradeNumber());
        bindNullable(statement, 10, entity.getIsin());
        bindNullable(statement, 11, entity.getIssuingCompanyAfcName());
        bindNullable(statement, 12, entity.getInstrumentAfcNormName());
        bindNullable(statement, 13, entity.getReceiptBankNumber());
        bindNullable(statement, 14, entity.getWithdrawRayanId());
        return statement;
    }

    private static void bindNullable(Statement statement, int index, Long value) {
        if (value == null) statement.bindNull(index, Long.class);
        else statement.bind(index, value);
    }

    private static void bindNullable(Statement statement, int index, Integer value) {
        if (value == null) statement.bindNull(index, Integer.class);
        else statement.bind(index, value);
    }

    private static void bindNullable(Statement statement, int index, UUID value) {
        if (value == null) statement.bindNull(index, UUID.class);
        else statement.bind(index, value);
    }

    private static void bindNullable(Statement statement, int index, String value) {
        if (value == null) statement.bindNull(index, String.class);
        else statement.bind(index, value);
    }
}
