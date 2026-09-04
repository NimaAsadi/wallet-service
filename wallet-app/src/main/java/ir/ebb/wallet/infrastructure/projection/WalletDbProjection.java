package ir.ebb.wallet.infrastructure.projection;

import ir.ebb.wallet.actor.WalletActor;
import ir.ebb.wallet.actor.event.WalletEvent;
import ir.ebb.wallet.projection.repository.WalletDebtRepository;
import ir.ebb.wallet.projection.repository.WalletRepository;
import ir.ebb.wallet.projection.repository.WalletTransactionRepository;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.ShardedDaemonProcessSettings;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.persistence.query.typed.EventEnvelope;
import org.apache.pekko.persistence.r2dbc.query.javadsl.R2dbcReadJournal;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.apache.pekko.projection.javadsl.ExactlyOnceProjection;
import org.apache.pekko.projection.javadsl.SourceProvider;
import org.apache.pekko.projection.r2dbc.R2dbcProjectionSettings;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcProjection;

import java.util.List;
import java.util.Optional;

/**
 * Starts the DB read-model projection for the next-gen {@link WalletActor}
 * (entity type {@code "WalletActor"}) over {@code eventsBySlices} (R2DBC).
 *
 * <p>The 1024 event slices (derived deterministically from each entity's persistence id) are
 * split into {@link #NUMBER_OF_PROCESSES} ranges; one {@code ShardedDaemonProcess} worker runs
 * per range. Read-model writes and the projection offset commit atomically in one R2DBC
 * transaction ({@code exactlyOnce}), so the handler may be replayed safely.
 *
 * <p>Unlike the old {@code eventsByTag} scheme, the number of ranges can be changed later (to
 * scale with the cluster) by stopping the old projections first; the journal is never
 * re-tagged. The R2DBC connection comes from {@code pekko.persistence.r2dbc.connection-factory}
 * (shared with the journal); no {@code DataSource} is needed here.
 *
 * <p>Per-event handling lives in {@link WalletDbProjectionHandler}; until those handlers are
 * implemented the projection parks (backoff-retries) on the first {@code WalletActor} event
 * rather than advancing its offset past it.
 */
public class WalletDbProjection {

    private static final String PROJECTION_NAME = "WalletDbProjection";
    /** Slice-range workers (1024 slices / N). Tune for cluster size; stop old projections first when changing. */
    private static final int NUMBER_OF_PROCESSES = 4;

    private final ActorSystem<?> system;
    private final WalletRepository walletRepository;
    private final WalletDebtRepository walletDebtRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletDbProjection(ActorSystem<?> system,
                              WalletRepository walletRepository,
                              WalletDebtRepository walletDebtRepository,
                              WalletTransactionRepository walletTransactionRepository) {
        this.system = system;
        this.walletRepository = walletRepository;
        this.walletDebtRepository = walletDebtRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    public void init() {
        List<Pair<Integer, Integer>> sliceRanges =
                EventSourcedProvider.sliceRanges(system, R2dbcReadJournal.Identifier(), NUMBER_OF_PROCESSES);

        ShardedDaemonProcess.get(system).init(
                ProjectionBehavior.Command.class,
                PROJECTION_NAME,
                NUMBER_OF_PROCESSES,
                processNumber -> ProjectionBehavior.create(createProjection(sliceRanges.get(processNumber))),
                ShardedDaemonProcessSettings.create(system),
                Optional.of(ProjectionBehavior.stopMessage()));
    }

    private ExactlyOnceProjection<Offset, EventEnvelope<WalletEvent>> createProjection(Pair<Integer, Integer> sliceRange) {
        int minSlice = sliceRange.first();
        int maxSlice = sliceRange.second();

        SourceProvider<Offset, EventEnvelope<WalletEvent>> sourceProvider =
                EventSourcedProvider.eventsBySlices(
                        system,
                        R2dbcReadJournal.Identifier(),
                        WalletActor.ENTITY_TYPE_KEY.name(),
                        minSlice,
                        maxSlice);

        return R2dbcProjection.exactlyOnce(
                ProjectionId.of(PROJECTION_NAME, WalletActor.ENTITY_TYPE_KEY.name() + "-db-" + minSlice + "-" + maxSlice),
                Optional.<R2dbcProjectionSettings>empty(),
                sourceProvider,
                () -> new WalletDbProjectionHandler(system, walletRepository, walletDebtRepository, walletTransactionRepository),
                system);
    }
}
