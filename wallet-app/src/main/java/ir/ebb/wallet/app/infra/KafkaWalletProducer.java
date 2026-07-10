package ir.ebb.wallet.app.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Replaces Spring Kafka's {@code KafkaTemplate}. Producer-only, keyed by a
 * String, with values serialized to JSON via Jackson (matching the original
 * {@code JsonSerializer} wire format). Fire-and-forget sends.
 */
@Slf4j
public class KafkaWalletProducer implements AutoCloseable {

    private final org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaWalletProducer(String bootstrapServers, ObjectMapper objectMapper) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        this.producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);
        this.objectMapper = objectMapper;
    }

    public void send(String topic, String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            producer.send(new ProducerRecord<>(topic, key, json));
        } catch (Exception e) {
            log.warn("Failed to send Kafka message to {}: {}", topic, e.getMessage());
        }
    }

    @Override
    public void close() {
        producer.close();
    }
}
