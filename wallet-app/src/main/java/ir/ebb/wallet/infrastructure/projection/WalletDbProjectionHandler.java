package ir.ebb.wallet.infrastructure.projection;

import io.r2dbc.spi.Statement;
import ir.ebb.wallet.actor.event.Deposited;
import ir.ebb.wallet.actor.event.Frozen;
import ir.ebb.wallet.actor.event.Spent;
import ir.ebb.wallet.actor.event.Unfrozen;
import ir.ebb.wallet.actor.event.WalletCreated;
import ir.ebb.wallet.actor.event.WalletEvent;
import ir.ebb.wallet.actor.event.Withdrew;
import ir.ebb.wallet.projection.adapter.WalletProjectionAdapter;
import ir.ebb.wallet.projection.repository.WalletDebtRepository;
import ir.ebb.wallet.projection.repository.WalletRepository;
import ir.ebb.wallet.projection.repository.WalletTransactionRepository;
import ir.ebb.wallet.valueobject.Wallet;
import ir.ebb.wallet.valueobject.WalletDebt;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.persistence.query.typed.EventEnvelope;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcHandler;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Read-model projection handler for the {@code WalletActor} delta events: routes each persisted
 * {@link WalletEvent} onto the {@code wallet} / {@code wallet_debt} / {@code wallet_transaction}
 * tables. Runs via {@code R2dbcProjection.exactlyOnce} (see {@link WalletDbProjection}), so
 * everything written through the {@link R2dbcSession} commits atomically with the projection
 * offset.
 *
 * <p><b>Identity is the account number, not the aggregate UUID.</b> The entity id is
 * {@code dbsAccountNumber + yyyyWW} (ISO week — see {@code WalletServiceImpl#entityId}), so a
 * <em>new</em> entity (new persistence id) is created per account per week and its
 * {@link WalletCreated} carries a <em>zeroed</em> aggregate. The read model therefore merges all
 * weekly entities of one account into a single row keyed by {@code dbsAccountNumber}: a
 * {@code WalletCreated} for an account that already has a row is a no-op (never overwrite the
 * accumulated balances with the zeroed weekly snapshot), and every delta event looks the row up
 * by account number — the only key all event types carry (their aggregate UUIDs rotate weekly).
 * This also keeps the projection coexistent with the legacy rows the live JDBC stack wrote into
 * the same tables.
 *
 * <p><b>Park, never skip.</b> Any failure — missing wallet row, read-model drift, a SQL/driver
 * error — completes the stage exceptionally, which rolls the transaction (including the offset)
 * back and makes the projection backoff-retry the same event. The offset only advances once the
 * event is fully applied.
 *
 * <p><b>Replay idempotency.</b> Each {@code wallet_transaction} leg gets the deterministic id
 * {@code nameUUIDFromBytes(persistenceId + ":" + sequenceNr + ":" + legIndex)} — a replayed
 * event produces identical ids (leg indices are stable because the {@code Wallet} waterfall
 * appends legs in a fixed order). Re-running the projection over a populated read model still
 * double-counts balances though: truncate the read-model tables when resetting offsets.
 *
 * <p>All statements of one event go through a single {@code session.update(List<Statement>)} —
 * sequential on the session's connection, failure short-circuits, committed with the offset.
 */
public class WalletDbProjectionHandler extends R2dbcHandler<EventEnvelope<WalletEvent>> {

    private final ActorSystem<?> system;
    private final WalletRepository walletRepository;
    private final WalletDebtRepository walletDebtRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletDbProjectionHandler(ActorSystem<?> system,
                                     WalletRepository walletRepository,
                                     WalletDebtRepository walletDebtRepository,
                                     WalletTransactionRepository walletTransactionRepository) {
        this.system = system;
        this.walletRepository = walletRepository;
        this.walletDebtRepository = walletDebtRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Override
    public CompletionStage<Done> process(R2dbcSession session, EventEnvelope<WalletEvent> envelope) {
        try {
            CompletionStage<Done> stage = switch (envelope.event()) {
                case WalletCreated event -> handleWalletCreated(session, event);
                case Deposited event -> updateWalletByAccountNumber(session, event.dbsAccountNumber(), envelope,
                        wallet -> wallet.deposit(event.trackingId(), event.value(), event.settlementDelay(), event.walletTransactionType()));
                case Frozen event -> updateWalletByAccountNumber(session, event.dbsAccountNumber(), envelope,
                        wallet -> wallet.freeze(event.trackingId(), event.value(), event.settlementDelay(), event.walletTransactionType(), event.canSpendSeparCredit()));
                case Spent event -> updateWalletByAccountNumber(session, event.dbsAccountNumber(), envelope,
                        wallet -> wallet.spend(event.trackingId(), event.value(), event.settlementDelay(), event.walletTransactionType()));
                case Unfrozen event -> updateWalletByAccountNumber(session, event.dbsAccountNumber(), envelope,
                        wallet -> wallet.unfreeze(event.trackingId(), event.value(), event.settlementDelay(), event.walletTransactionType()));
                case Withdrew event -> updateWalletByAccountNumber(session, event.dbsAccountNumber(), envelope,
                        wallet -> wallet.withdraw(event.trackingId(), event.value(), event.settlementDelay(), event.walletTransactionType()));
                // WalletEvent is not sealed → a default branch is mandatory. Unknown future event
                // types are logged and skipped (the offset still advances past them).
                default -> {
                    system.log().atInfo().log("WalletDbProjectionHandler received {} but no handler was set for it. Skipped.",
                            envelope.event().getClass().getName());
                    yield CompletableFuture.completedFuture(Done.getInstance());
                }
            };
            return stage.whenComplete((done, error) -> {
                if (error != null) logPark(envelope, error);
            });
        } catch (RuntimeException error) {
            logPark(envelope, error);
            return CompletableFuture.failedFuture(error);
        }
    }

    /**
     * First event of every weekly entity: creates the read-model row pair (wallet + its 1:1
     * {@code wallet_debt}) on the entity's <em>first</em> appearance only. A row for the account
     * that already exists — from an earlier week's entity or the legacy stack — means this is a
     * rotation/replay: the event carries a zeroed aggregate, so the correct merge is a no-op.
     */
    private CompletionStage<Done> handleWalletCreated(R2dbcSession session, WalletCreated event) {
        var aggregate = Objects.requireNonNull(event.wallet(), "WalletCreated#wallet must not be null");
        Long accountNumber = Objects.requireNonNull(aggregate.getDbsAccountNumber(),
                "WalletCreated without dbsAccountNumber (lossy aggregate serialization)");
        return walletRepository.selectOneByAccountNumberWithDebt(session, accountNumber)
                .thenCompose(existing -> {
                    if (existing.isPresent()) return CompletableFuture.completedFuture(Done.getInstance());

                    var walletEntity = WalletProjectionAdapter.adapt(WalletProjectionAdapter.adapt(aggregate));
                    if (walletEntity.getId() == null)
                        walletEntity.setId(UUID.nameUUIDFromBytes(("wallet:" + accountNumber)
                                .getBytes(StandardCharsets.UTF_8)));
                    var debt = Objects.requireNonNullElseGet(aggregate.getWalletDebt(), WalletDebt::new);
                    var debtEntity = WalletProjectionAdapter.adapt(debt, walletEntity.getId());
                    return session.update(List.of(
                                    walletRepository.insertStatement(session, walletEntity),
                                    walletDebtRepository.insertStatement(session, debtEntity)))
                            .thenApply(__ -> Done.getInstance());
                });
    }

    /**
     * Applies one delta event to the account's read-model row: load (by {@code dbsAccountNumber})
     * → replay the same {@code Wallet} waterfall the aggregate ran → write everything back
     * (wallet UPDATE, {@code wallet_debt} INSERT-if-absent-or-UPDATE, one row per transaction
     * leg) in one sequential, offset-atomic batch.
     */
    private CompletionStage<Done> updateWalletByAccountNumber(
            R2dbcSession session,
            Long dbsAccountNumber,
            EventEnvelope<WalletEvent> envelope,
            ThrowingConsumer<Wallet> applyChanges
    ) {
        Objects.requireNonNull(dbsAccountNumber, "event without dbsAccountNumber");
        return walletRepository.selectOneByAccountNumberWithDebt(session, dbsAccountNumber)
                .thenCompose(existing -> {
                    if (existing.isEmpty())
                        throw new CompletionException(new IllegalStateException(
                                "Wallet row not found for account " + dbsAccountNumber
                                        + " — parking " + envelope.persistenceId() + "#" + envelope.sequenceNr()));
                    var walletWithDebt = existing.get();
                    var wallet = WalletProjectionAdapter.adapt(walletWithDebt);
                    try {
                        applyChanges.accept(wallet);
                    } catch (Exception e) {
                        // ApplicationException (read-model drift vs the aggregate) and anything else:
                        // fail the stage → rollback + retry, never advance the offset.
                        throw new CompletionException("Read-model update failed for account " + dbsAccountNumber, e);
                    }

                    var now = LocalDateTime.now();
                    var walletEntity = WalletProjectionAdapter.adapt(wallet);
                    walletEntity.setUpdatedAt(now);
                    var debtEntity = WalletProjectionAdapter.adapt(wallet.getWalletDebt(), walletEntity.getId());
                    debtEntity.setUpdatedAt(now);

                    var statements = new ArrayList<Statement>();
                    statements.add(walletRepository.updateStatement(session, walletEntity));
                    // A legacy wallet row can predate the 1:1 wallet_debt row: mapRowWithDebt marks
                    // that absence with a zeroed entity whose createdAt stayed null — insert it now.
                    if (walletWithDebt.walletDebt().getCreatedAt() == null)
                        statements.add(walletDebtRepository.insertStatement(session, debtEntity));
                    else
                        statements.add(walletDebtRepository.updateStatement(session, debtEntity));

                    var legs = wallet.getWalletTransactions();
                    String legIdSeed = envelope.persistenceId() + ":" + envelope.sequenceNr() + ":";
                    for (int i = 0; i < legs.size(); i++) {
                        UUID legId = UUID.nameUUIDFromBytes((legIdSeed + i).getBytes(StandardCharsets.UTF_8));
                        statements.add(walletTransactionRepository.insertStatement(session,
                                WalletProjectionAdapter.adapt(legs.get(i), legId)));
                    }
                    return session.update(statements).thenApply(__ -> Done.getInstance());
                });
    }

    private void logPark(EventEnvelope<WalletEvent> envelope, Throwable error) {
        system.log().atError().log("WalletDbProjection parking on {}#{} ({}): {}",
                envelope.persistenceId(), envelope.sequenceNr(),
                envelope.event().getClass().getSimpleName(), error);
    }

    /**
     * {@link java.util.function.Consumer} that may throw — the {@code Wallet} waterfall methods
     * declare the checked {@code ApplicationException}, and a lambda cannot propagate it.
     */
    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }
}
