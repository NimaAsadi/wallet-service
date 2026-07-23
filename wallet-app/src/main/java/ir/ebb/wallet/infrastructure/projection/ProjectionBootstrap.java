package ir.ebb.wallet.infrastructure.projection;

import ir.ebb.wallet.app.infra.KafkaWalletProducer;
import ir.ebb.wallet.wallet.WalletActor;
import ir.ebb.wallet.wallet.WalletEvent;
import ir.ebb.wallet.wallet.WalletReadModelProjection;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.persistence.r2dbc.query.javadsl.R2dbcReadJournal;
import org.apache.pekko.projection.Projection;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.persistence.query.typed.EventEnvelope;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.apache.pekko.projection.javadsl.SourceProvider;
import org.apache.pekko.projection.r2dbc.R2dbcProjectionSettings;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcProjection;

import java.util.List;
import java.util.Optional;

/**
 * Starts the wallet read-model + Kafka projections over {@code eventsBySlices} (R2DBC).
 *
 * <p>The 1024 event slices (derived deterministically from each wallet's persistence id) are
 * split into {@link #NUM_SLICE_RANGES} ranges; one {@code ShardedDaemonProcess} worker runs per
 * range, for each projection:
 * <ul>
 *   <li>{@code wallet-read-model} — exactlyOnce UPSERT of {@code wallet}/{@code wallet_debt}
 *       + INSERT {@code wallet_transaction} (offset + read model committed atomically).</li>
 *   <li>{@code wallet-kafka} — atLeastOnce publish of {@code wallet.state.updated}.</li>
 * </ul>
 * Unlike the old {@code eventsByTag} scheme, the number of ranges can be changed later (to scale
 * with the cluster) by stopping the old projections first; the journal is never re-tagged.
 *
 * <p>The R2DBC connection comes from {@code pekko.persistence.r2dbc.connection-factory} (shared
 * with the journal); no {@code DataSource} is needed here.
 */
public final class ProjectionBootstrap {

    public static final String READ_JOURNAL_ID = R2dbcReadJournal.Identifier();
    private static final String READ_MODEL_NAME = "wallet-read-model";
    private static final String KAFKA_NAME = "wallet-kafka";
    /** Number of slice ranges (= projection workers per projection). Tune for cluster size. */
    private static final int NUM_SLICE_RANGES = 16;

    private final ActorSystem<?> system;
    private final KafkaWalletProducer kafka;
    private final String stateTopic;

    public ProjectionBootstrap(ActorSystem<?> system, KafkaWalletProducer kafka, String stateTopic) {
        this.system = system;
        this.kafka = kafka;
        this.stateTopic = stateTopic;
    }

    public void start() {
        String entityType = WalletActor.ENTITY_TYPE_KEY.name();
        List<Pair<Integer, Integer>> sliceRanges =
                EventSourcedProvider.sliceRanges(system, READ_JOURNAL_ID, NUM_SLICE_RANGES);

        ShardedDaemonProcess.get(system).init(
                ProjectionBehavior.Command.class,
                READ_MODEL_NAME,
                sliceRanges.size(),
                idx -> ProjectionBehavior.create(readModelProjection(entityType, sliceRanges.get(idx))));

        ShardedDaemonProcess.get(system).init(
                ProjectionBehavior.Command.class,
                KAFKA_NAME,
                sliceRanges.size(),
                idx -> ProjectionBehavior.create(kafkaProjection(entityType, sliceRanges.get(idx))));
    }

    private Projection<EventEnvelope<WalletEvent>> readModelProjection(
            String entityType, Pair<Integer, Integer> sliceRange) {
        int minSlice = sliceRange.first();
        int maxSlice = sliceRange.second();
        SourceProvider<Offset, EventEnvelope<WalletEvent>> source =
                EventSourcedProvider.eventsBySlices(system, READ_JOURNAL_ID, entityType, minSlice, maxSlice);
        return R2dbcProjection.exactlyOnce(
                ProjectionId.of(READ_MODEL_NAME, minSlice + "-" + maxSlice),
                Optional.<R2dbcProjectionSettings>empty(),
                source,
                WalletReadModelProjection::new,
                system);
    }

    private Projection<EventEnvelope<WalletEvent>> kafkaProjection(
            String entityType, Pair<Integer, Integer> sliceRange) {
        int minSlice = sliceRange.first();
        int maxSlice = sliceRange.second();
        SourceProvider<Offset, EventEnvelope<WalletEvent>> source =
                EventSourcedProvider.eventsBySlices(system, READ_JOURNAL_ID, entityType, minSlice, maxSlice);
        return R2dbcProjection.atLeastOnce(
                ProjectionId.of(KAFKA_NAME, minSlice + "-" + maxSlice),
                Optional.<R2dbcProjectionSettings>empty(),
                source,
                () -> new WalletKafkaProjection(kafka, stateTopic),
                system);
    }
}
