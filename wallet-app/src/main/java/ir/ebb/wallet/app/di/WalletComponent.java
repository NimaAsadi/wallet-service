package ir.ebb.wallet.app.di;

import com.typesafe.config.Config;
import dagger.Component;
import ir.ebb.wallet.app.infra.KafkaWalletProducer;
import ir.ebb.wallet.app.web.GrpcServer;
import ir.ebb.wallet.app.web.WalletHttpServer;
import ir.ebb.wallet.app.web.WalletJobs;
import ir.ebb.wallet.infrastructure.projection.WalletDbProjection;
import ir.ebb.wallet.repository.WalletRepository;
import ir.ebb.wallet.wallet.WalletFacade;
import org.apache.pekko.actor.typed.ActorSystem;

import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * Composition root. {@code Main} pulls the lifecycle-relevant singletons from here and
 * orchestrates startup (Liquibase → ActorSystem → Pekko Management → Cluster Bootstrap →
 * Cluster Sharding → projection → seed → HTTP → gRPC → cron) and the reverse-order shutdown
 * hook — all construction itself is resolved by this graph.
 */
@Singleton
@Component(modules = {
        ConfigModule.class,
        PersistenceModule.class,
        PekkoModule.class,
        DomainModule.class,
        RayanModule.class,
        KafkaModule.class,
        WebModule.class,
        JobsModule.class})
public interface WalletComponent {

    // ── lifecycle / bootstrap ──────────────────────────────────────────────────
    DataSource dataSource();          // Main's first request: Liquibase runs inside the @Provides
    ActorSystem<?> actorSystem();
    Config config();

    // ── managed subsystems (init/start called by Main) ─────────────────────────
    WalletDbProjection walletDbProjection();
    WalletHttpServer httpServer();
    GrpcServer grpcServer();
    WalletJobs walletJobs();

    // ── shutdown-hook handles + seeder inputs ──────────────────────────────────
    KafkaWalletProducer kafkaWalletProducer();
    WalletRepository walletRepository(); // JDBC read repo — LegacySeeder input
    WalletFacade walletFacade();
}
