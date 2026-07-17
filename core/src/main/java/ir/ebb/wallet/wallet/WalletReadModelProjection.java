package ir.ebb.wallet.wallet;

import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.wallet.aggregate.WalletTransaction;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.jdbc.JdbcSession;
import org.apache.pekko.projection.jdbc.javadsl.JdbcHandler;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CQRS read-model projection. Consumes persisted {@link WalletEvent}s from an
 * {@code EventsByTag} stream and updates the {@code wallet} + {@code wallet_debt} +
 * {@code wallet_transaction} read tables. Run via {@code JdbcProjection.exactlyOnce} so the
 * read model and the projection offset advance atomically (no duplicates).
 *
 * <p>Idempotent: {@code wallet}/{@code wallet_debt} are upserted from the event's absolute
 * resulting {@link WalletState} (re-applying is a no-op); each audit leg is inserted with a
 * deterministic {@code id} derived from (persistenceId, sequenceNr, legIndex) so a replay
 * never duplicates a transaction row.
 *
 * <p><b>Blocking JDBC is expected here</b>: this handler runs on {@code wallet-blocking-dispatcher}
 * (configured via {@code pekko.projection.jdbc.use-dispatcher}), which isolates the blocking
 * Postgres round-trips from the actor system / HTTP threads. The leg inserts are batched into a
 * single {@code executeBatch} to minimise blocking time and connection hold time per envelope.
 */
public class WalletReadModelProjection extends JdbcHandler<EventEnvelope<WalletEvent>, JdbcSession> {

    private static final String UPSERT_WALLET = """
            INSERT INTO wallet (id, version, user_id, account_number,
                t0_balance, t0_frozen, t1_balance, t1_frozen, t2_balance, t2_frozen,
                credit, initial_credit, separ_credit, separ_initial_credit, created_at, updated_at)
            VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
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
            VALUES (?, ?, ?, ?, now(), now())
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
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """;

    @Override
    public void process(JdbcSession session, EventEnvelope<WalletEvent> envelope) throws Exception {
        WalletEvent event = envelope.event();
        WalletState state = stateOf(event);
        if (state == null || !state.isCreated()) {
            return;
        }
        List<WalletTransaction> legs = event instanceof WalletEvent.WalletMutated m ? m.legs() : List.of();
        session.withConnection((Connection conn) -> {
            Jdbc.update(conn, UPSERT_WALLET,
                    state.id(),
                    state.user() != null ? state.user().getKeycloakId() : null,
                    state.accountNumber(),
                    state.t0().balance(), state.t0().frozen(),
                    state.t1().balance(), state.t1().frozen(),
                    state.t2().balance(), state.t2().frozen(),
                    state.credit(), state.initialCredit(), state.separCredit(), state.separInitialCredit());
            Jdbc.update(conn, UPSERT_WALLET_DEBT,
                    state.id(), state.debt().t2Tot0(), state.debt().t2Tot1(), state.debt().t1Tot0());
            // Batch the homogeneous leg inserts into one round-trip (N legs → 1 executeBatch).
            List<Object[]> txBatches = new ArrayList<>(legs.size());
            for (int idx = 0; idx < legs.size(); idx++) {
                WalletTransaction leg = legs.get(idx);
                UUID txId = UUID.nameUUIDFromBytes(
                        (envelope.persistenceId() + ":" + envelope.sequenceNr() + ":" + idx)
                                .getBytes(StandardCharsets.UTF_8));
                txBatches.add(new Object[]{
                        txId,
                        leg.getUser() != null ? leg.getUser().getKeycloakId() : null,
                        leg.getUser() != null ? leg.getUser().getDbsAccountNumber() : state.accountNumber(),
                        leg.getWalletId(),
                        leg.getWalletOperationType(),
                        leg.getWalletTransactionType(),
                        leg.getWalletParameterType(),
                        leg.getAmount(),
                        leg.getTrackingId(),
                        leg.getFrozenBefore(), leg.getFrozenAfter(),
                        leg.getBalanceBefore(), leg.getBalanceAfter()
                });
            }
            Jdbc.batchUpdate(conn, INSERT_TX, txBatches);
            return null;
        });
    }

    private static WalletState stateOf(WalletEvent event) {
        return switch (event) {
            case WalletEvent.WalletCreated e -> e.initialState();
            case WalletEvent.WalletMutated e -> e.resultingState();
            case WalletEvent.WalletSeeded e -> e.state();
        };
    }
}
