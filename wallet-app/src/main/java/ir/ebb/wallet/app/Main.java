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
import ir.ebb.wallet.actor.WalletActorService;
import ir.ebb.wallet.actor.WalletRegistryActor;
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
import ir.ebb.wallet.repository.WalletRepository;
import ir.ebb.wallet.repository.credit.CreditHistoryRepository;
import ir.ebb.wallet.repository.transaction.WalletTransactionRepository;
import ir.ebb.wallet.repository.turnover.TurnoverRepository;
import ir.ebb.wallet.service.command.WalletCommandService;
import ir.ebb.wallet.service.command.WalletCommandServiceImpl;
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
import ir.ebb.userinfo.repository.UserRepository;
import ir.ebb.userinfo.service.query.UserQueryService;
import ir.ebb.userinfo.service.query.UserQueryServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.ServerBinding;

import javax.sql.DataSource;

/**
 * Composition root replacing Spring DI + {@code WalletApplication}. Wires
 * dependencies explicitly in order: config → HikariCP → Liquibase → JDBC repos →
 * domain services → ActorSystem → Rayan client → web services → HTTP/gRPC
 * servers → cron jobs, then blocks on ActorSystem termination. A JVM shutdown
 * hook tears everything down in reverse.
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
        UserRepository userRepository = new UserRepository(dataSource);
        ir.ebb.external.rayan.wallet.repository.RayanWalletRepository rayanWalletRepository =
                new ir.ebb.external.rayan.wallet.repository.RayanWalletRepository(dataSource);
        ir.ebb.external.rayan.wallet.repository.RayanWalletHistoryRepository rayanWalletHistoryRepository =
                new ir.ebb.external.rayan.wallet.repository.RayanWalletHistoryRepository(dataSource);

        // 3. domain services
        WalletCommandService walletCommandService = new WalletCommandServiceImpl(
                dataSource, walletRepository, walletTransactionRepository, creditHistoryRepository);
        WalletQueryService walletQueryService = new WalletQueryServiceImpl(walletRepository);
        TurnoverQueryService turnoverQueryService = new TurnoverQueryServiceImpl(turnoverRepository);
        TurnoverCommandService turnoverCommandService = new TurnoverCommandServiceImpl(turnoverRepository);
        CreditHistoryQueryService creditHistoryQueryService = new CreditHistoryQueryServiceImpl(creditHistoryRepository);
        WalletTransactionQueryService walletTransactionQueryService = new WalletTransactionQueryServiceImpl(walletTransactionRepository);
        UserQueryService userQueryService = new UserQueryServiceImpl(userRepository);

        // 4. actor system (rooted at WalletRegistryActor)
        ActorSystem<WalletRegistryActor.Command> actorSystem =
                ActorSystem.create(WalletRegistryActor.create(walletCommandService), "wallet-actor-system", config);

        // 5. Rayan external integration
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

        // 6. actor facade + admin job service
        WalletActorService walletActorService = new WalletActorService(actorSystem, walletQueryService, walletCommandService);
        RayanWalletJobService rayanWalletJobService = new RayanWalletJobServiceImpl(
                walletCommandService, walletQueryService,
                rayanWalletCommandService, rayanWalletHistoryCommandService, turnoverCommandService);

        // 7. messaging
        KafkaWalletProducer kafkaWalletProducer = new KafkaWalletProducer(
                config.getString("wallet.kafka.bootstrap-servers"), objectMapper);

        // 8. web services
        WalletWebService walletWebService = new WalletWebServiceImpl(walletQueryService);
        TurnoverWebService turnoverWebService = new TurnoverWebServiceImpl(turnoverQueryService);
        BridgeWalletWebService bridgeWalletWebService = new BridgeWalletWebServiceImpl(walletQueryService);
        BridgeTurnoverWebService bridgeTurnoverWebService = new BridgeTurnoverWebServiceImpl(turnoverQueryService, walletQueryService);
        BidarDepositWalletWebService bidarDepositWalletWebService = new BidarDepositWalletWebServiceImpl(walletActorService, userQueryService);
        boolean activeCredit = config.getBoolean("wallet.credit.active");
        AdminWalletWebService adminWalletWebService = new AdminWalletWebServiceImpl(
                walletCommandService, walletQueryService, rayanWalletQueryService, rayanWalletCommandService,
                creditHistoryQueryService, userQueryService, turnoverCommandService, activeCredit);
        AdminWalletTransactionWebService adminWalletTransactionWebService =
                new AdminWalletTransactionWebServiceImpl(walletTransactionQueryService);
        TurnoverNotifyWebService turnoverNotifyWebService = new TurnoverNotifyWebServiceImpl(
                turnoverQueryService, walletQueryService, kafkaWalletProducer,
                config.getString("wallet.kafka.topic.turnover-notification"));

        // 9. HTTP server
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

        // 10. gRPC server
        GrpcServer grpcServer = new GrpcServer(
                config.getInt("wallet.grpc.port"),
                new WalletGrpcServiceImpl(walletQueryService),
                new BridgeGrpcAuthInterceptor(bridgeVerifier));
        grpcServer.start();

        // 11. scheduled jobs
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

        // 12. shutdown hook
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
