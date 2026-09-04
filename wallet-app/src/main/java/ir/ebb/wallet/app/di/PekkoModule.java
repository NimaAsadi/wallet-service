package ir.ebb.wallet.app.di;

import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

import javax.inject.Singleton;

@Module
public abstract class PekkoModule {

    /** Cluster guardian root (no actor registry) — identical to the pre-Dagger Main. */
    @Provides
    @Singleton
    public static ActorSystem<?> actorSystem(Config config) {
        return ActorSystem.create(Behaviors.<String>ignore(), "wallet-actor-system", config);
    }
}
