package ir.ebb.wallet.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ir.ebb.external.rayan.configuration.RayanHttpClient;
import ir.ebb.external.rayan.configuration.RayanResult;
import ir.ebb.external.rayan.login.RayanLoginService;
import ir.ebb.external.rayan.login.impl.RayanLoginServiceImpl;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandService;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandServiceImpl;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletHistoryCommandService;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletHistoryCommandServiceImpl;
import ir.ebb.external.rayan.wallet.service.query.RayanWalletQueryService;
import ir.ebb.external.rayan.wallet.service.query.RayanWalletQueryServiceImpl;
import ir.ebb.wallet.app.admin.service.AdminWalletTransactionWebService;
import ir.ebb.wallet.app.admin.service.AdminWalletTransactionWebServiceImpl;
import ir.ebb.wallet.app.admin.service.AdminWalletWebService;
import ir.ebb.wallet.app.admin.service.AdminWalletWebServiceImpl;
import ir.ebb.wallet.app.admin.service.RayanWalletJobService;
import ir.ebb.wallet.app.admin.service.RayanWalletJobServiceImpl;
import ir.ebb.wallet.app.admin.service.job.TurnoverNotifyWebService;
import ir.ebb.wallet.app.admin.service.job.TurnoverNotifyWebServiceImpl;
import ir.ebb.wallet.app.bridge.bidardeposit.service.BidarDepositWalletWebService;
import ir.ebb.wallet.app.bridge.bidardeposit.service.BidarDepositWalletWebServiceImpl;
import ir.ebb.wallet.app.bridge.grpc.WalletGrpcServiceImpl;
import ir.ebb.wallet.app.bridge.service.BridgeTurnoverWebService;
import ir.ebb.wallet.app.bridge.service.BridgeTurnoverWebServiceImpl;
import ir.ebb.wallet.app.bridge.service.BridgeWalletWebService;
import ir.ebb.wallet.app.bridge.service.BridgeWalletWebServiceImpl;
import ir.ebb.wallet.app.infra.DataSourceProvider;
import ir.ebb.wallet.app.infra.KafkaWalletProducer;
import ir.ebb.wallet.app.infra.LiquibaseMigrator;
import ir.ebb.wallet.app.user.service.TurnoverWebService;
import ir.ebb.wallet.app.user.service.TurnoverWebServiceImpl;
import ir.ebb.wallet.app.user.service.WalletWebService;
import ir.ebb.wallet.app.user.service.WalletWebServiceImpl;
import ir.ebb.wallet.app.web.BridgeGrpcAuthInterceptor;
import ir.ebb.wallet.app.web.CronScheduler;
import ir.ebb.wallet.app.web.GrpcServer;
import ir.ebb.wallet.app.web.JwtVerifier;
import ir.ebb.wallet.app.web.WalletHttpServer;
import ir.ebb.wallet.app.web.WalletJobs;
import ir.ebb.wallet.infrastructure.migration.LegacySeeder;
import ir.ebb.wallet.repository.WalletRepository;
import ir.ebb.wallet.repository.credit.CreditHistoryRepository;
import ir.ebb.wallet.repository.transaction.WalletTransactionRepository;
import ir.ebb.wallet.repository.turnover.TurnoverRepository;
import ir.ebb.wallet.service.credit.query.CreditHistoryQueryService;
import ir.ebb.wallet.service.credit.query.CreditHistoryQueryServiceImpl;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.query.WalletQueryServiceImpl;
import ir.ebb.wallet.service.transaction.query.WalletTransactionQueryService;
import ir.ebb.wallet.service.transaction.query.WalletTransactionQueryServiceImpl;
import ir.ebb.wallet.service.turnover.command.TurnoverCommandService;
import ir.ebb.wallet.service.turnover.command.TurnoverCommandServiceImpl;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryServiceImpl;
import ir.ebb.wallet.wallet.WalletActor;
import ir.ebb.wallet.wallet.WalletFacade;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap;
import org.apache.pekko.management.javadsl.PekkoManagement;

import javax.sql.DataSource;

/**
 * Composition root replacing Spring DI + the legacy actor-registry root. Wires
 * dependencies explicitly: config → HikariCP → Liquibase → JDBC repos → domain services →
 * Pekko {@link ActorSystem} (cluster guardian) → Cluster Bootstrap + {@link ClusterSharding}
 * (the {@link WalletActor})  → {@link WalletFacade} → Rayan →
 * web services → HTTP/gRPC servers → cron jobs, then blocks on ActorSystem termination. A JVM
 * shutdown hook tears everything down in reverse.
 *
 * <p>There is no actor registry: wallet entities are located exclusively via Cluster Sharding
 * ({@code WalletFacade} → {@code sharding.entityRefFor("wallet", accountNumber)}). The event
 * journal is the source of truth; reads are served by the CQRS projections.
 */
@Slf4j
public class Main {

    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // 1. persistence
        DataSource dataSource = DataSourceProvider.hikari(
                config.getString("wallet.db.url"),
                config.getString("wallet.db.username"),
                config.getString("wallet.db.password"),
                config.getInt("wallet.db.pool-size"));
        LiquibaseMigrator.migrate(dataSource);
        log.info("Liquibase migrations applied");

        // 2. JDBC repos
        WalletRepository walletRepository = new WalletRepository(dataSource);
        WalletTransactionRepository walletTransactionRepository = new WalletTransactionRepository(dataSource);
        CreditHistoryRepository creditHistoryRepository = new CreditHistoryRepository(dataSource);
        TurnoverRepository turnoverRepository = new TurnoverRepository(dataSource);
        ir.ebb.external.rayan.wallet.repository.RayanWalletRepository rayanWalletRepository =
                new ir.ebb.external.rayan.wallet.repository.RayanWalletRepository(dataSource);
        ir.ebb.external.rayan.wallet.repository.RayanWalletHistoryRepository rayanWalletHistoryRepository =
                new ir.ebb.external.rayan.wallet.repository.RayanWalletHistoryRepository(dataSource);

        // 3. domain services (read side + turnover; wallet writes now go through the WalletFacade entity)
        WalletQueryService walletQueryService = new WalletQueryServiceImpl(walletRepository);
        TurnoverQueryService turnoverQueryService = new TurnoverQueryServiceImpl(turnoverRepository);
        TurnoverCommandService turnoverCommandService = new TurnoverCommandServiceImpl(turnoverRepository);
        CreditHistoryQueryService creditHistoryQueryService = new CreditHistoryQueryServiceImpl(creditHistoryRepository);
        WalletTransactionQueryService walletTransactionQueryService = new WalletTransactionQueryServiceImpl(walletTransactionRepository);

        // 4. actor system (cluster guardian root; no actor registry)
        ActorSystem<String> actorSystem =
                ActorSystem.create(Behaviors.<String>ignore(), "wallet-actor-system", config);

        // 5. cluster: management HTTP (:8558) → Cluster Bootstrap (kubernetes-api) → sharding → projections
        PekkoManagement.get(actorSystem).start().toCompletableFuture().join();
        log.info("Pekko Management HTTP started on :{}", config.getInt("pekko.management.http.port"));
        ClusterBootstrap.get(actorSystem).start();
        ClusterSharding.get(actorSystem).init(
                Entity.of(WalletActor.ENTITY_TYPE_KEY, WalletActor::create).withRole("wallet"));
        log.info("Cluster Sharding initialized for wallet entity");

        // 6. messaging
        KafkaWalletProducer kafkaWalletProducer = new KafkaWalletProducer(
                config.getString("wallet.kafka.bootstrap-servers"), objectMapper);
        String walletStateTopic = config.getString("wallet.kafka.topic.wallet-state");

        // 8. write-side facade over the sharded, event-sourced WalletEntity
        WalletFacade walletFacade = new WalletFacade(actorSystem);

        // 8a. one-off legacy seed (--seed-from-legacy): import existing wallet rows into the journal.
        if (java.util.Arrays.asList(args).contains("--seed-from-legacy")) {
            LegacySeeder.run(walletRepository, walletFacade);
        }

        // 9. Rayan external integration
        RayanHttpClient rayanHttpClient = new RayanHttpClient(
                actorSystem, config.getString("wallet.rayan.base-url"), objectMapper);
        RayanLoginService rayanLoginService = new RayanLoginServiceImpl(
                rayanHttpClient,
                config.getString("wallet.rayan.auth.username"),
                config.getString("wallet.rayan.auth.password"),
                config.getString("wallet.rayan.auth.application-key"));
        RayanResult.configure(objectMapper, rayanLoginService);
        RayanWalletQueryService rayanWalletQueryService = new RayanWalletQueryServiceImpl(rayanLoginService, rayanHttpClient, rayanWalletRepository);
        RayanWalletCommandService rayanWalletCommandService = new RayanWalletCommandServiceImpl(rayanLoginService, rayanHttpClient, rayanWalletRepository, dataSource);
        RayanWalletHistoryCommandService rayanWalletHistoryCommandService = new RayanWalletHistoryCommandServiceImpl(rayanWalletHistoryRepository, dataSource);

        // 10. admin job service (Rayan sync reconciles sharded wallet entities toward Rayan snapshots)
        RayanWalletJobService rayanWalletJobService = new RayanWalletJobServiceImpl(
                walletFacade, walletQueryService,
                rayanWalletCommandService, rayanWalletHistoryCommandService, turnoverCommandService);

        // 11. web services
        WalletWebService walletWebService = new WalletWebServiceImpl(walletFacade);
        TurnoverWebService turnoverWebService = new TurnoverWebServiceImpl(turnoverQueryService);
        BridgeWalletWebService bridgeWalletWebService = new BridgeWalletWebServiceImpl(walletFacade);
        BridgeTurnoverWebService bridgeTurnoverWebService = new BridgeTurnoverWebServiceImpl(turnoverQueryService, walletQueryService);
        BidarDepositWalletWebService bidarDepositWalletWebService = new BidarDepositWalletWebServiceImpl(walletFacade);
        boolean activeCredit = config.getBoolean("wallet.credit.active");
        AdminWalletWebService adminWalletWebService = new AdminWalletWebServiceImpl(
                walletFacade, walletQueryService, rayanWalletQueryService, rayanWalletCommandService,
                creditHistoryQueryService, creditHistoryRepository, activeCredit);
        AdminWalletTransactionWebService adminWalletTransactionWebService =
                new AdminWalletTransactionWebServiceImpl(walletTransactionQueryService);
        TurnoverNotifyWebService turnoverNotifyWebService = new TurnoverNotifyWebServiceImpl(
                turnoverQueryService, walletQueryService, kafkaWalletProducer,
                config.getString("wallet.kafka.topic.turnover-notification"));

        // 12. HTTP server
        JwtVerifier userVerifier = new JwtVerifier(config.getString("wallet.keycloak.user.jwk-set-uri"));
        JwtVerifier adminVerifier = new JwtVerifier(config.getString("wallet.keycloak.admin.jwk-set-uri"));
        JwtVerifier bridgeVerifier = new JwtVerifier(config.getString("wallet.keycloak.bridge.jwk-set-uri"));
        WalletHttpServer httpServer = new WalletHttpServer(
                objectMapper, userVerifier, adminVerifier, bridgeVerifier,
                walletWebService, turnoverWebService, bridgeWalletWebService, bridgeTurnoverWebService,
                bidarDepositWalletWebService, adminWalletWebService, adminWalletTransactionWebService);
        ServerBinding httpBinding = Http.get(actorSystem)
                .newServerAt("0.0.0.0", config.getInt("wallet.http.port"))
                .bind(httpServer.routes())
                .toCompletableFuture().join();
        log.info("HTTP server bound on {}", httpBinding.localAddress());

        // 13. gRPC server
        GrpcServer grpcServer = new GrpcServer(
                config.getInt("wallet.grpc.port"),
                new WalletGrpcServiceImpl(walletFacade),
                new BridgeGrpcAuthInterceptor(bridgeVerifier));
        grpcServer.start();

        // 14. scheduled jobs
        CronScheduler cronScheduler = new CronScheduler(actorSystem);
        new WalletJobs(cronScheduler, rayanWalletQueryService, rayanWalletJobService,
                rayanWalletHistoryCommandService, turnoverNotifyWebService,
                config.getString("wallet.schedule.rayan"),
                config.getString("wallet.schedule.rayan-backup"),
                config.getString("wallet.schedule.rayan-remove"),
                config.getString("wallet.schedule.turnover-notify"))
                .scheduleAll();

        log.info("wallet-service started (HTTP :{}, gRPC :{})",
                httpBinding.localAddress().getPort(), config.getInt("wallet.grpc.port"));

        // 15. shutdown hook
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
