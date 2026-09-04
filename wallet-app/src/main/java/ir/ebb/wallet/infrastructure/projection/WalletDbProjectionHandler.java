package ir.ebb.wallet.infrastructure.projection;

import ir.ebb.wallet.actor.event.*;
import ir.ebb.wallet.projection.adapter.WalletProjectionAdapter;
import ir.ebb.wallet.projection.repository.WalletDebtRepository;
import ir.ebb.wallet.projection.repository.WalletRepository;
import ir.ebb.wallet.projection.repository.WalletTransactionRepository;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.persistence.query.typed.EventEnvelope;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcHandler;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Dispatch skeleton for the {@code WalletActor} read-model projection: routes each persisted
 * {@link WalletEvent} to its per-event handler stub. Runs via {@code R2dbcProjection.exactlyOnce}
 * (see {@link WalletDbProjection}), so everything written through the {@link R2dbcSession} commits
 * atomically with the projection offset.
 *
 * <p><b>Stubs.</b> Every {@code handle*} method throws {@link UnsupportedOperationException} on
 * purpose: until the real handlers land, the projection must park (backoff-retry) on an event
 * instead of silently advancing its offset past it. Implement each stub, then delete this note.
 *
 * <p><b>Cross-cutting notes for the implementations:</b>
 * <ul>
 *   <li>Batch several statements (wallet upsert + debt + transaction insert) in one
 *       {@code session.update(List<Statement>)} — they commit with the offset in one transaction.</li>
 *   <li>Deterministic {@code wallet_transaction} id for replay idempotency:
 *       {@code UUID.nameUUIDFromBytes((envelope.persistenceId() + ":" + envelope.sequenceNr())
 *       .getBytes(StandardCharsets.UTF_8))} — the pattern proven by the old read-model projection.</li>
 *   <li>Generated repositories bind nullable fields directly (no {@code bindNull}) and emit plain
 *       INSERTs (no {@code ON CONFLICT}) — null {@code userId}/{@code frozenBefore}/… will throw on
 *       the Postgres R2DBC driver, so keep them populated and handle conflicts yourself.</li>
 *   <li>{@code updateStatement} binds {@code updatedAt} — populate it before updating.</li>
 *   <li>Write the {@code wallet} row before any {@code wallet_transaction} row
 *       ({@code wallet_transaction.wallet_id} references {@code wallet(id)}).</li>
 * </ul>
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
        return switch (envelope.event()) {
            case WalletCreated event -> handleWalletCreated(session, event, envelope);
            case Deposited event -> handleDeposited(session, event, envelope);
            case Frozen event -> handleFrozen(session, event, envelope);
            case Spent event -> handleSpent(session, event, envelope);
            case Unfrozen event -> handleUnfrozen(session, event, envelope);
            case Withdrew event -> handleWithdrew(session, event, envelope);
            // WalletEvent is not sealed → a default branch is mandatory. Unknown future event
            // types are logged and skipped (the offset still advances past them).
            default -> {
                system.log().info("WalletDbProjectionHandler received {} but no handler was set for it. Skipped.",
                        envelope.event().getClass().getName());
                yield CompletableFuture.completedFuture(Done.getInstance());
            }
        };
    }

    /**
     * TODO: first event of every wallet entity — inserts the {@code wallet} (+ {@code wallet_debt}) row.
     *
     * <p><b>Mind the entity rotation:</b> the entity id is {@code dbsAccountNumber + yyyyWW} (ISO
     * week), so a <em>new</em> entity (new persistence id) is created per account per week and its
     * {@link WalletCreated} carries a <em>zeroed</em> {@code WalletAggregate}. Merge state by
     * {@code dbsAccountNumber} across the weekly entities — never naively upsert the zeroed
     * snapshot over the accumulated read-model row.
     *
     * <p>The event carries no wallet UUID and no user id ({@code WalletEntity.id}/{@code userId}
     * must be derived — e.g. deterministic UUID per {@code dbsAccountNumber} — or looked up).
     *
     * <p>{@code WalletAggregate} does not currently round-trip through Fastjson2 (private fields,
     * no getters/no-arg ctor → serializes as {@code {}}); fix that in the actor migration before
     * this handler can trust the embedded aggregate.
     */
    private CompletionStage<Done> handleWalletCreated(
            R2dbcSession session,
            WalletCreated event,
            EventEnvelope<WalletEvent> envelope
    ) {
        var wallet = WalletProjectionAdapter.adapt(event.wallet());
        var walletEntity = WalletProjectionAdapter.adapt(wallet);
        return walletRepository.insert(session, walletEntity)
                .thenApply(__ -> Done.getInstance());
    }

    /**
     * TODO: credit deposit — delta event.
     *
     * <p>Apply to the read model per {@code settlementDelay}: the deposit waterfall restores
     * {@code separCredit} → {@code credit} → tier balance (mirror {@code WalletAggregate.applyEvent(Deposited)}),
     * then insert a {@code wallet_transaction} leg (deterministic id, {@code amount = event.value()},
     * {@code trackingId}) and upsert {@code wallet_debt} if {@code settleDebt} moved counters.
     */
    private CompletionStage<Done> handleDeposited(
            R2dbcSession session,
            Deposited event,
            EventEnvelope<WalletEvent> envelope
    ) {
        return walletRepository.selectOne(session, event.walletId())
                .thenComposeAsync(optionalWallet -> {
                    if (optionalWallet.isEmpty()) {
                        system.log().atError().log("WalletEntity#{} not found", event.walletId());
                        return CompletableFuture.completedFuture(Done.getInstance());
                    }

                    return walletDebtRepository.selectOne(session, event.walletId())
                            .thenComposeAsync(optionalWalletDebt -> {
                                if (optionalWalletDebt.isEmpty()) {
                                    system.log().atError().log("WalletDebtEntity#{} not found", event.walletId());
                                    return CompletableFuture.completedFuture(Done.getInstance());
                                }

                                var wallet = WalletProjectionAdapter.adapt(optionalWallet.get(), optionalWalletDebt.get());
                                wallet.deposit(event.trackingId(), event.value(), event.settlementDelay(), event.walletTransactionType());

                                var walletSaveFuture = walletRepository.save(session, WalletProjectionAdapter.adapt(wallet));
                                var walletDebtSaveFuture = walletDebtRepository.save(session, WalletProjectionAdapter.adapt(wallet.getWalletDebt(), event.walletId()));
                                var walletTransactionSaveFutures = wallet.getWalletTransactions().stream()
                                        .map(walletTransaction ->
                                                walletTransactionRepository.save(session, WalletProjectionAdapter.adapt(walletTransaction)))
                                        .collect(Collectors.toCollection(ArrayList::new));
                                walletTransactionSaveFutures.add(walletSaveFuture);
                                walletTransactionSaveFutures.add(walletDebtSaveFuture);

                                return CompletableFuture.allOf(
                                                walletTransactionSaveFutures.stream().map(CompletionStage::toCompletableFuture).toArray(CompletableFuture[]::new))
                                        .thenApply(__ -> Done.getInstance());
                            });
                });
    }

    /**
     * TODO: freeze — delta event.
     *
     * <p>Apply per {@code settlementDelay}: raise the tier's {@code frozen} (borrowing across
     * tiers per {@code canSpendSeparCredit} — mirror {@code WalletAggregate.applyEvent(Frozen)}),
     * upsert {@code wallet_debt} for any lender-borrowing, insert the {@code wallet_transaction}
     * leg with {@code frozenBefore}/{@code frozenAfter}.
     */
    private CompletionStage<Done> handleFrozen(R2dbcSession session, Frozen event, EventEnvelope<WalletEvent> envelope) {
        throw new UnsupportedOperationException("TODO: implement Frozen read-model handling");
    }

    /**
     * TODO: spend against frozen funds — delta event.
     *
     * <p>Apply per {@code settlementDelay}: lower the tier's {@code frozen} (spending may draw on
     * separ credit per {@code canSpendSeparCredit} — mirror {@code WalletAggregate.applyEvent(Spent)}),
     * insert the {@code wallet_transaction} leg with {@code frozenBefore}/{@code frozenAfter}.
     */
    private CompletionStage<Done> handleSpent(R2dbcSession session, Spent event, EventEnvelope<WalletEvent> envelope) {
        throw new UnsupportedOperationException("TODO: implement Spent read-model handling");
    }

    /**
     * TODO: release frozen funds — delta event.
     *
     * <p>Composite in the aggregate ({@code Spent} + {@code Deposited} pair): lower the tier's
     * {@code frozen} and return the value to balance/credit per {@code settlementDelay} (mirror
     * {@code WalletAggregate.applyEvent(Unfrozen)}), insert the {@code wallet_transaction} leg.
     */
    private CompletionStage<Done> handleUnfrozen(R2dbcSession session, Unfrozen event, EventEnvelope<WalletEvent> envelope) {
        throw new UnsupportedOperationException("TODO: implement Unfrozen read-model handling");
    }

    /**
     * TODO: withdraw from tier balance — delta event.
     *
     * <p>Apply per {@code settlementDelay}: lower the tier's balance (mirror
     * {@code WalletAggregate.applyEvent(Withdrew)}), insert the {@code wallet_transaction} leg
     * with {@code balanceBefore}/{@code balanceAfter}.
     */
    private CompletionStage<Done> handleWithdrew(R2dbcSession session, Withdrew event, EventEnvelope<WalletEvent> envelope) {
        throw new UnsupportedOperationException("TODO: implement Withdrew read-model handling");
    }
}
