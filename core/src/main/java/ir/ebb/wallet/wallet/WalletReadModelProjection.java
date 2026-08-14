package ir.ebb.wallet.wallet;

import ir.ebb.wallet.valueobject.WalletTransaction;
import org.apache.pekko.Done;
import org.apache.pekko.persistence.query.typed.EventEnvelope;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcHandler;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;
import io.r2dbc.spi.Statement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * CQRS read-model projection. Consumes persisted {@link WalletEvent}s from an
 * {@code eventsBySlices} stream and updates the {@code wallet} + {@code wallet_debt} +
 * {@code wallet_transaction} read tables. Run via {@code R2dbcProjection.exactlyOnce} so the
 * read model and the projection offset advance atomically (no duplicates).
 *
 * <p>Idempotent: {@code wallet}/{@code wallet_debt} are upserted from the event's absolute
 * resulting {@link WalletState} (re-applying is a no-op); each audit leg is inserted with a
 * deterministic {@code id} derived from (persistenceId, sequenceNr, legIndex) so a replay
 * never duplicates a transaction row.
 *
 * <p><b>Reactive R2DBC</b>: statements run non-blocking via {@link R2dbcSession}. The three
 * statements (upsert wallet, upsert debt, batched transaction insert) are submitted in one
 * {@link R2dbcSession#update(List)} call, committed with the offset in the same transaction.
 * Postgres R2DBC uses {@code $1,$2,...} placeholders (not JDBC {@code ?}) and requires
 * {@code bindNull} for nulls (it cannot infer their SQL type).
 */
public class WalletReadModelProjection extends R2dbcHandler<EventEnvelope<WalletEvent>> {

    private static final String UPSERT_WALLET = """
            INSERT INTO wallet (id, version, user_id, account_number,
                t0_balance, t0_frozen, t1_balance, t1_frozen, t2_balance, t2_frozen,
                credit, initial_credit, separ_credit, separ_initial_credit, created_at, updated_at)
            VALUES ($1, 0, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, now(), now())
            ON CONFLICT (id) DO UPDATE SET
                t0_balance = EXCLUDED.t0_balance, t0_frozen = EXCLUDED.t0_frozen,
                t1_balance = EXCLUDED.t1_balance, t1_frozen = EXCLUDED.t1_frozen,
                t2_balance = EXCLUDED.t2_balance, t2_frozen = EXCLUDED.t2_frozen,
                credit = EXCLUDED.credit, initial_credit = EXCLUDED.initial_credit,
                separ_credit = EXCLUDED.separ_credit, separ_initial_credit = EXCLUDED.separ_initial_credit,
                updated_at = now()
            """;

    // wallet_debt keeps only the 3 ever-incremented counters after V004 dropped the credit/separ cols.
    private static final String UPSERT_WALLET_DEBT = """
            INSERT INTO wallet_debt (wallet_id, t2_to_t0_debt, t2_to_t1_debt, t1_to_t0_debt, created_at, updated_at)
            VALUES ($1, $2, $3, $4, now(), now())
            ON CONFLICT (wallet_id) DO UPDATE SET
                t2_to_t0_debt = EXCLUDED.t2_to_t0_debt,
                t2_to_t1_debt = EXCLUDED.t2_to_t1_debt,
                t1_to_t0_debt = EXCLUDED.t1_to_t0_debt,
                updated_at = now()
            """;

    private static final String INSERT_TX = """
            INSERT INTO wallet_transaction (id, user_id, account_number, wallet_id,
                wallet_operation_type, wallet_transaction_type, wallet_parameter_type,
                amount, tracking_id, frozen_before, frozen_after, balance_before, balance_after)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
            ON CONFLICT (id) DO NOTHING
            """;

    @Override
    public CompletionStage<Done> process(R2dbcSession session, EventEnvelope<WalletEvent> envelope) {
        WalletEvent event = envelope.event();
        WalletState state = stateOf(event);
        if (state == null || !state.isCreated()) {
            return CompletableFuture.completedFuture(Done.getInstance());
        }
        List<WalletTransaction> legs = event instanceof WalletEvent.WalletMutated m ? m.legs() : List.of();

        // wallet + wallet_debt first (wallet_transaction FKs reference wallet(id)).
        List<Statement> statements = new ArrayList<>(3);
        statements.add(bindWallet(session.createStatement(UPSERT_WALLET), state));
        statements.add(bindDebt(session.createStatement(UPSERT_WALLET_DEBT), state));

        if (!legs.isEmpty()) {
            // Batch the homogeneous leg inserts into one statement (N legs → N batched executions).
            Statement txStmt = session.createStatement(INSERT_TX);
            for (int idx = 0; idx < legs.size(); idx++) {
                WalletTransaction leg = legs.get(idx);
                UUID txId = UUID.nameUUIDFromBytes(
                        (envelope.persistenceId() + ":" + envelope.sequenceNr() + ":" + idx)
                                .getBytes(StandardCharsets.UTF_8));
                bindLeg(txStmt, txId, leg, state);
                txStmt.add();
            }
            statements.add(txStmt);
        }

        return session.update(statements).thenApply(rowsUpdated -> Done.getInstance());
    }

    private static Statement bindWallet(Statement stmt, WalletState state) {
        stmt.bind(0, state.id());
        bindUuid(stmt, 1, state.user() != null ? state.user().getKeycloakId() : null);
        stmt.bind(2, state.accountNumber());
        stmt.bind(3, state.t0().balance());
        stmt.bind(4, state.t0().frozen());
        stmt.bind(5, state.t1().balance());
        stmt.bind(6, state.t1().frozen());
        stmt.bind(7, state.t2().balance());
        stmt.bind(8, state.t2().frozen());
        stmt.bind(9, state.credit());
        stmt.bind(10, state.initialCredit());
        stmt.bind(11, state.separCredit());
        stmt.bind(12, state.separInitialCredit());
        return stmt;
    }

    private static Statement bindDebt(Statement stmt, WalletState state) {
        stmt.bind(0, state.id());
        stmt.bind(1, state.debt().t2Tot0());
        stmt.bind(2, state.debt().t2Tot1());
        stmt.bind(3, state.debt().t1Tot0());
        return stmt;
    }

    private static Statement bindLeg(Statement stmt, UUID txId, WalletTransaction leg, WalletState state) {
        Long accountNumber;
        if (leg.getUser() != null) {
            accountNumber = leg.getUser().getDbsAccountNumber();
        } else {
            accountNumber = state.accountNumber();
        }
        stmt.bind(0, txId);
        bindUuid(stmt, 1, leg.getUser() != null ? leg.getUser().getKeycloakId() : null);
        bindLong(stmt, 2, accountNumber);
        bindUuid(stmt, 3, leg.getWalletId());
        stmt.bind(4, leg.getWalletOperationType().name());
        stmt.bind(5, leg.getWalletTransactionType().name());
        stmt.bind(6, leg.getWalletParameterType().name());
        bindLong(stmt, 7, leg.getAmount());
        bindUuid(stmt, 8, leg.getTrackingId());
        bindLong(stmt, 9, leg.getFrozenBefore());
        bindLong(stmt, 10, leg.getFrozenAfter());
        bindLong(stmt, 11, leg.getBalanceBefore());
        bindLong(stmt, 12, leg.getBalanceAfter());
        return stmt;
    }

    private static void bindUuid(Statement stmt, int index, UUID value) {
        if (value == null) {
            stmt.bindNull(index, UUID.class);
        } else {
            stmt.bind(index, value);
        }
    }

    private static void bindLong(Statement stmt, int index, Long value) {
        if (value == null) {
            stmt.bindNull(index, Long.class);
        } else {
            stmt.bind(index, value);
        }
    }

    private static WalletState stateOf(WalletEvent event) {
        return switch (event) {
            case WalletEvent.WalletCreated e -> e.initialState();
            case WalletEvent.WalletMutated e -> e.resultingState();
            case WalletEvent.WalletSeeded e -> e.state();
            // WalletCreated2 / WalletDeposited are sealed-permitted but never persisted — the
            // projection only ever sees the three real events. Defensive default for exhaustiveness.
            default -> throw new IllegalStateException("Unexpected WalletEvent: " + event);
        };
    }
}
