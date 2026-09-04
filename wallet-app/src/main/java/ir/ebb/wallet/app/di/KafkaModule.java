package ir.ebb.wallet.app.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import ir.ebb.wallet.app.infra.KafkaWalletProducer;

import javax.inject.Singleton;

@Module
public abstract class KafkaModule {

    @Provides
    @Singleton
    public static KafkaWalletProducer kafkaWalletProducer(Config config, ObjectMapper objectMapper) {
        return new KafkaWalletProducer(config.getString("wallet.kafka.bootstrap-servers"), objectMapper);
    }
}
