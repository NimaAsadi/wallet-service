package ir.ebb.wallet.projection.repository;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.Statement;
import ir.ebb.wallet.projection.entity.WalletDebtEntity;
import ir.ebb.wallet.projection.entity.WalletEntity;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Singleton
public class WalletRepository extends BaseWalletRepository {

    /** Explicit for Dagger — the inherited default ctor is invisible to annotation processing. */
    @Inject
    public WalletRepository() {
    }

    /**
     * 1:1 join with {@code wallet_debt} (its PK {@code wallet_id} is the FK to {@code wallet.id}).
     * Explicit column list — not {@code SELECT *} — so the labels stay stable; only the debt-side
     * columns colliding with the wallet side ({@code created_at}/{@code updated_at}) are aliased
     * ({@code debt_*}), every other label stays bare so {@link #mapRow(Row)} can map the wallet
     * side unchanged.
     */
    private static final String SELECT_BY_ID_WITH_DEBT_STATEMENT = """
            SELECT w.id, w.version, w.account_number,
                   w.t0_balance, w.t0_frozen, w.t1_balance, w.t1_frozen, w.t2_balance, w.t2_frozen,
                   w.credit, w.initial_credit, w.separ_credit, w.separ_initial_credit,
                   w.created_at, w.updated_at,
                   d.wallet_id, d.t2_to_t0_debt, d.t2_to_t1_debt, d.t1_to_t0_debt,
                   d.t2_to_credit_debt, d.t1_to_credit_debt, d.t2_to_separ_credit_debt, d.t1_to_separ_credit_debt,
                   d.created_at AS debt_created_at, d.updated_at AS debt_updated_at
            FROM "wallet" w LEFT JOIN "wallet_debt" d ON d.wallet_id = w.id
            WHERE w.id = $1""";

    public Statement selectByIdWithDebtStatement(R2dbcSession session, UUID id) {
        return session.createStatement(SELECT_BY_ID_WITH_DEBT_STATEMENT).bind(0, id);
    }

    public CompletionStage<Optional<WalletWithDebt>> selectOneWithDebt(R2dbcSession session, UUID id) {
        return session.selectOne(selectByIdWithDebtStatement(session, id), this::mapRowWithDebt);
    }

    /**
     * Same joined row, keyed by {@code account_number} — the read model merges the
     * {@code dbsAccountNumber + yyyyWW} weekly entities of one account into a single row, so the
     * delta handlers look the wallet up by account number, not by the (weekly, rotating)
     * aggregate UUID. {@code ORDER BY created_at LIMIT 1} keeps the pick deterministic if
     * duplicate legacy rows for one account ever appear.
     */
    private static final String SELECT_BY_ACCOUNT_NUMBER_WITH_DEBT_STATEMENT = """
            SELECT w.id, w.version, w.account_number,
                   w.t0_balance, w.t0_frozen, w.t1_balance, w.t1_frozen, w.t2_balance, w.t2_frozen,
                   w.credit, w.initial_credit, w.separ_credit, w.separ_initial_credit,
                   w.created_at, w.updated_at,
                   d.wallet_id, d.t2_to_t0_debt, d.t2_to_t1_debt, d.t1_to_t0_debt,
                   d.t2_to_credit_debt, d.t1_to_credit_debt, d.t2_to_separ_credit_debt, d.t1_to_separ_credit_debt,
                   d.created_at AS debt_created_at, d.updated_at AS debt_updated_at
            FROM "wallet" w LEFT JOIN "wallet_debt" d ON d.wallet_id = w.id
            WHERE w.account_number = $1
            ORDER BY w.created_at ASC
            LIMIT 1""";

    public CompletionStage<Optional<WalletWithDebt>> selectOneByAccountNumberWithDebt(R2dbcSession session, long accountNumber) {
        var statement = session.createStatement(SELECT_BY_ACCOUNT_NUMBER_WITH_DEBT_STATEMENT)
                .bind(0, accountNumber);
        return session.selectOne(statement, this::mapRowWithDebt);
    }

    /**
     * Maps the joined row onto both entities (the wallet side through the generated
     * {@link #mapRow(Row)} — same bare labels). A missing {@code wallet_debt} row (LEFT JOIN,
     * {@code wallet_id} null) yields a zeroed {@link WalletDebtEntity} with {@code walletId} set
     * from the wallet row — the wallet itself is never dropped by the join.
     */
    WalletWithDebt mapRowWithDebt(Row row) {
        WalletEntity wallet = mapRow(row);
        WalletDebtEntity debt = new WalletDebtEntity();
        UUID walletId = row.get("wallet_id", UUID.class);
        if (walletId != null) {
            debt.walletId = walletId;
            debt.t2ToT0Debt = row.get("t2_to_t0_debt", Long.class);
            debt.t2ToT1Debt = row.get("t2_to_t1_debt", Long.class);
            debt.t1ToT0Debt = row.get("t1_to_t0_debt", Long.class);
            debt.t2ToCreditDebt = row.get("t2_to_credit_debt", Long.class);
            debt.t1ToCreditDebt = row.get("t1_to_credit_debt", Long.class);
            debt.t2ToSeparCreditDebt = row.get("t2_to_separ_credit_debt", Long.class);
            debt.t1ToSeparCreditDebt = row.get("t1_to_separ_credit_debt", Long.class);
            debt.createdAt = row.get("debt_created_at", LocalDateTime.class);
            debt.updatedAt = row.get("debt_updated_at", LocalDateTime.class);
        } else {
            debt.walletId = wallet.id;
        }
        return new WalletWithDebt(wallet, debt);
    }
}
