package ir.ebb.wallet.infrastructure.projection;

import ir.ebb.wallet.app.infra.KafkaWalletProducer;
import ir.ebb.wallet.wallet.WalletEvent;
import ir.ebb.wallet.wallet.WalletReadModelProjection;
import ir.ebb.wallet.wallet.WalletTags;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.javadsl.ShardedDaemonProcess;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.apache.pekko.projection.Projection;
import org.apache.pekko.projection.javadsl.SourceProvider;
import org.apache.pekko.projection.jdbc.JdbcSession;
import org.apache.pekko.projection.jdbc.javadsl.JdbcProjection;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Supplier;

/**
 * Starts the wallet read-model + Kafka projections. For each of {@link WalletTags#allTags()}
 * (16 tags) it runs an {@code EventsByTag} source through a {@code ShardedDaemonProcess}
 * worker (one per tag):
 * <ul>
 *   <li>{@code wallet-read-model} — exactlyOnce UPSERT of {@code wallet}/{@code wallet_debt}
 *       + INSERT {@code wallet_transaction} (offset + read model atomic).</li>
 *   <li>{@code wallet-kafka} — atLeastOnce publish of {@code wallet.state.updated}.</li>
 * </ul>
 */
public final class ProjectionBootstrap {

    public static final String READ_JOURNAL_ID = "jdbc-read-journal";
    private static final String READ_MODEL_NAME = "wallet-read-model";
    private static final String KAFKA_NAME = "wallet-kafka";

    private final ActorSystem<?> system;
    private final DataSource dataSource;
    private final KafkaWalletProducer kafka;
    private final String stateTopic;

    public ProjectionBootstrap(ActorSystem<?> system, DataSource dataSource,
                               KafkaWalletProducer kafka, String stateTopic) {
        this.system = system;
        this.dataSource = dataSource;
        this.kafka = kafka;
        this.stateTopic = stateTopic;
    }

    public void start() {
        List<String> tags = WalletTags.allTags();
        Supplier<JdbcSession> sessionFactory = () -> new HikariJdbcSession(dataSource);

        ShardedDaemonProcess.get(system).init(
                ProjectionBehavior.Command.class,
                READ_MODEL_NAME,
                tags.size(),
                idx -> ProjectionBehavior.create(readModelProjection(tags.get(idx), sessionFactory)));

        ShardedDaemonProcess.get(system).init(
                ProjectionBehavior.Command.class,
                KAFKA_NAME,
                tags.size(),
                idx -> ProjectionBehavior.create(kafkaProjection(tags.get(idx), sessionFactory)));
    }

    private Projection<EventEnvelope<WalletEvent>> readModelProjection(String tag, Supplier<JdbcSession> sessionFactory) {
        SourceProvider<Offset, EventEnvelope<WalletEvent>> source =
                EventSourcedProvider.eventsByTag(system, tag, READ_JOURNAL_ID);
        return JdbcProjection.<Offset, EventEnvelope<WalletEvent>, JdbcSession>exactlyOnce(
                ProjectionId.of(READ_MODEL_NAME, tag),
                source,
                sessionFactory,
                WalletReadModelProjection::new,
                system);
    }

    private Projection<EventEnvelope<WalletEvent>> kafkaProjection(String tag, Supplier<JdbcSession> sessionFactory) {
        SourceProvider<Offset, EventEnvelope<WalletEvent>> source =
                EventSourcedProvider.eventsByTag(system, tag, READ_JOURNAL_ID);
        return JdbcProjection.<Offset, EventEnvelope<WalletEvent>, JdbcSession>atLeastOnce(
                ProjectionId.of(KAFKA_NAME, tag),
                source,
                sessionFactory,
                () -> new WalletKafkaProjection(kafka, stateTopic),
                system);
    }
}
