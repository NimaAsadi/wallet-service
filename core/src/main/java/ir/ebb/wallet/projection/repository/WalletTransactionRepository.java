package ir.ebb.wallet.projection.repository;

import io.r2dbc.spi.Statement;
import ir.ebb.wallet.projection.entity.WalletTransactionEntity;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.UUID;

/**
 * Null-safe binding for the {@code wallet_transaction} INSERT. The generated
 * {@code BaseWalletTransactionRepository#saveStatement} binds every column directly, but the
 * Postgres R2DBC driver rejects null binds — and real legs carry nulls: an unfreeze's first leg
 * has all four before/after audit columns null, credit legs have null {@code frozenBefore}/
 * {@code frozenAfter}. Overriding {@code saveStatement} covers every write path
 * ({@code insertStatement} delegates to it), nulling the audit columns + {@code trackingId} via
 * {@code bindNull}; the identity/enum columns stay guarded (a leg without them is a programming
 * error, not a schema null).
 */
@Singleton
public class WalletTransactionRepository extends BaseWalletTransactionRepository {

    /** Explicit for Dagger — the inherited default ctor is invisible to annotation processing. */
    @Inject
    public WalletTransactionRepository() {
    }

    /** Copied verbatim from the generated repository (its constant is private). */
    private static final String INSERT_STATEMENT = "INSERT INTO \"wallet_transaction\"(id, version, account_number, wallet_id, wallet_operation_type, wallet_transaction_type, wallet_parameter_type, amount, tracking_id, frozen_before, frozen_after, balance_before, balance_after) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)";

    @Override
    public Statement saveStatement(R2dbcSession session, WalletTransactionEntity entity) {
        Statement statement = session.createStatement(INSERT_STATEMENT)
                .bind(0, Objects.requireNonNull(entity.id, "id"))
                .bind(1, entity.version)
                .bind(2, entity.accountNumber)
                .bind(3, Objects.requireNonNull(entity.walletId, "walletId"))
                .bind(4, Objects.requireNonNull(entity.walletOperationType, "walletOperationType").name())
                .bind(5, Objects.requireNonNull(entity.walletTransactionType, "walletTransactionType").name())
                .bind(6, Objects.requireNonNull(entity.walletParameterType, "walletParameterType").name())
                .bind(7, entity.amount);
        bindNullable(statement, 8, entity.trackingId);
        bindNullable(statement, 9, entity.frozenBefore);
        bindNullable(statement, 10, entity.frozenAfter);
        bindNullable(statement, 11, entity.balanceBefore);
        bindNullable(statement, 12, entity.balanceAfter);
        return statement;
    }

    private static void bindNullable(Statement statement, int index, Long value) {
        if (value == null) statement.bindNull(index, Long.class);
        else statement.bind(index, value);
    }

    private static void bindNullable(Statement statement, int index, UUID value) {
        if (value == null) statement.bindNull(index, UUID.class);
        else statement.bind(index, value);
    }
}
