package ir.ebb.wallet.app.config.kafka;

import ir.ebb.wallet.aggregate.Wallet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.wallet.state:wallet.state.updated}")
    private String walletStateTopic;

    @Bean
    public Consumer<Wallet> walletStatePublisher(KafkaTemplate<String, Wallet> kafkaTemplate) {
        return wallet -> {
            try {
                String key = wallet.getUser().getDbsAccountNumber().toString();
                kafkaTemplate.send(walletStateTopic, key, wallet);
            } catch (Exception e) {
                log.atWarn().log("Failed to publish wallet state for user {}: {}",
                        wallet.getUser().getDbsAccountNumber(), e.getMessage());
            }
        };
    }
}
