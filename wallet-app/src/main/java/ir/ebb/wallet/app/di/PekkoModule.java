package ir.ebb.wallet.app.di;

import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import ir.ebb.wallet.service.BlockingExecutor;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;

import javax.inject.Singleton;
import java.util.concurrent.Executor;

@Module
public abstract class PekkoModule {

    /** Cluster guardian root (no actor registry) — identical to the pre-Dagger Main. */
    @Provides
    @Singleton
    public static ActorSystem<?> actorSystem(Config config) {
        return ActorSystem.create(Behaviors.<String>ignore(), "wallet-actor-system", config);
    }

    /**
     * Safe before {@code Main}'s {@code ClusterSharding.init} — {@code get()} only resolves the
     * extension; entity refs are created lazily per command.
     */
    @Provides
    @Singleton
    public static ClusterSharding clusterSharding(ActorSystem<?> system) {
        return ClusterSharding.get(system);
    }

    /** The pre-configured {@code wallet-blocking-dispatcher}, for all blocking JDBC/Rayan work. */
    @Provides
    @Singleton
    @BlockingExecutor
    public static Executor blockingExecutor(ActorSystem<?> system) {
        return Adapter.toClassic(system).dispatchers().lookup("wallet-blocking-dispatcher");
    }
}
