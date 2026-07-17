package ir.ebb.wallet.infrastructure.projection;

import ir.ebb.wallet.app.infra.KafkaWalletProducer;
import ir.ebb.wallet.wallet.WalletEvent;
import ir.ebb.wallet.wallet.WalletState;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.jdbc.JdbcSession;
import org.apache.pekko.projection.jdbc.javadsl.JdbcHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes {@code wallet.state.updated} from each persisted {@link WalletEvent}, keyed by
 * account number. Runs as a separate at-least-once projection (best-effort: the producer is
 * fire-and-forget) so the DB read model can stay exactly-once. The {@link JdbcSession} is
 * used only to advance the projection offset.
 */
public class WalletKafkaProjection extends JdbcHandler<EventEnvelope<WalletEvent>, JdbcSession> {

    private final KafkaWalletProducer kafka;
    private final String topic;

    public WalletKafkaProjection(KafkaWalletProducer kafka, String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void process(JdbcSession session, EventEnvelope<WalletEvent> envelope) {
        WalletState state = stateOf(envelope.event());
        if (state == null || !state.isCreated()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("walletId", state.id());
        payload.put("accountNumber", state.accountNumber());
        payload.put("t0Balance", state.t0().balance());
        payload.put("t1Balance", state.t1().balance());
        payload.put("t2Balance", state.t2().balance());
        payload.put("credit", state.credit());
        payload.put("initialCredit", state.initialCredit());
        payload.put("separCredit", state.separCredit());
        payload.put("separInitialCredit", state.separInitialCredit());
        kafka.send(topic, String.valueOf(state.accountNumber()), payload);
    }

    private static WalletState stateOf(WalletEvent event) {
        return switch (event) {
            case WalletEvent.WalletCreated e -> e.initialState();
            case WalletEvent.WalletMutated e -> e.resultingState();
            case WalletEvent.WalletSeeded e -> e.state();
        };
    }
}
