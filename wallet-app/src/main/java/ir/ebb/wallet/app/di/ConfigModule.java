package ir.ebb.wallet.app.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/** The two leaf singletons every other binding descends from. */
@Module
public abstract class ConfigModule {

    @Provides
    @Singleton
    public static Config config() {
        return ConfigFactory.load();
    }

    @Provides
    @Singleton
    public static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
