package ir.ebb.wallet.app;

import com.typesafe.config.Config;
import ir.ebb.wallet.app.di.DaggerWalletComponent;
import ir.ebb.wallet.app.di.WalletComponent;
import ir.ebb.wallet.app.infra.KafkaWalletProducer;
import ir.ebb.wallet.app.web.GrpcServer;
import ir.ebb.wallet.app.web.WalletHttpServer;
import ir.ebb.wallet.app.web.WalletJobs;
import ir.ebb.wallet.infrastructure.migration.LegacySeeder;
import ir.ebb.wallet.wallet.WalletActor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap;
import org.apache.pekko.management.javadsl.PekkoManagement;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Lifecycle orchestration only — all wiring lives in {@link WalletComponent}. Startup order:
 * HikariCP + Liquibase (inside the DataSource binding, forced first) → Pekko {@link ActorSystem}
 * (cluster guardian) → Pekko Management → Cluster Bootstrap → {@link ClusterSharding} (the
 * {@link WalletActor}) → read-model projection → optional legacy seed → HTTP → gRPC → cron
 * jobs, then blocks on ActorSystem termination. A JVM shutdown hook tears everything down in
 * reverse.
 *
 * <p>There is no actor registry: wallet entities are located exclusively via Cluster Sharding
 * ({@code WalletFacade} → {@code sharding.entityRefFor("wallet", accountNumber)}). The event
 * journal is the source of truth; bulk reads are served by the read-model tables the projection
 * populates.
 */
@Slf4j
public class Main {

    public static void main(String[] args) throws Exception {
        WalletComponent container = DaggerWalletComponent.builder().build();

        // 1. persistence — requesting the DataSource first runs Liquibase inside the binding,
        //    so the schema is migrated before the actor system, repos, or projections exist.
        DataSource dataSource = container.dataSource();
        log.info("Liquibase migrations applied");

        // 2. actor system (cluster guardian root; no actor registry) + cluster stack
        ActorSystem<?> actorSystem = container.actorSystem();
        Config config = container.config();
        PekkoManagement.get(actorSystem).start().toCompletableFuture().join();
        log.info("Pekko Management HTTP started on :{}", config.getInt("pekko.management.http.port"));
        ClusterBootstrap.get(actorSystem).start();
        ClusterSharding.get(actorSystem).init(
                Entity.of(WalletActor.ENTITY_TYPE_KEY, WalletActor::create).withRole("wallet"));
        log.info("Cluster Sharding initialized for wallet entity");

        // 3. next-gen wallet read-model projection (WalletActor events → wallet/wallet_debt/wallet_transaction)
        container.walletDbProjection().init();
        log.info("WalletDbProjection initialized for entity type WalletActor");

        // 4. one-off legacy seed (--seed-from-legacy): import existing wallet rows into the journal.
        if (Arrays.asList(args).contains("--seed-from-legacy")) {
            LegacySeeder.run(container.walletRepository(), container.walletFacade());
        }

        // 5. HTTP server
        WalletHttpServer httpServer = container.httpServer();
        ServerBinding httpBinding = Http.get(actorSystem)
                .newServerAt("0.0.0.0", config.getInt("wallet.http.port"))
                .bind(httpServer.routes())
                .toCompletableFuture().join();
        log.info("HTTP server bound on {}", httpBinding.localAddress());

        // 6. gRPC server
        GrpcServer grpcServer = container.grpcServer();
        grpcServer.start();

        // 7. scheduled jobs
        WalletJobs walletJobs = container.walletJobs();
        walletJobs.scheduleAll();

        log.info("wallet-service started (HTTP :{}, gRPC :{})",
                httpBinding.localAddress().getPort(), config.getInt("wallet.grpc.port"));

        // 8. shutdown hook (reverse order)
        KafkaWalletProducer kafkaWalletProducer = container.kafkaWalletProducer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down wallet-service");
            httpBinding.unbind().toCompletableFuture().join();
            grpcServer.stop();
            actorSystem.terminate();
            kafkaWalletProducer.close();
            try { ((com.zaxxer.hikari.HikariDataSource) dataSource).close(); } catch (Exception ignore) {}
        }));

        actorSystem.getWhenTerminated().toCompletableFuture().join();
    }
}
