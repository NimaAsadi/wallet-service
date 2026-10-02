package ir.ebb.wallet.app.di;

import com.typesafe.config.Config;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
import ir.ebb.wallet.infrastructure.KafkaWalletProducer;
import ir.ebb.wallet.app.user.service.TurnoverWebService;
import ir.ebb.wallet.app.user.service.TurnoverWebServiceImpl;
import ir.ebb.wallet.app.user.service.WalletWebService;
import ir.ebb.wallet.app.user.service.WalletWebServiceImpl;
import ir.ebb.wallet.app.web.BridgeGrpcAuthInterceptor;
import ir.ebb.wallet.app.web.GrpcServer;
import ir.ebb.wallet.app.web.JwtVerifier;
import ir.ebb.wallet.service.BlockingExecutor;
import ir.ebb.wallet.service.WalletService;
import ir.ebb.wallet.service.credit.command.CreditHistoryCommandService;
import ir.ebb.wallet.service.credit.query.CreditHistoryQueryService;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;

import javax.inject.Singleton;
import java.util.concurrent.Executor;

/** Web services (user/bridge/admin audiences), JWT verifiers, and the gRPC server. */
@Module
public abstract class WebModule {

    // ── JWT verifiers — three instances of the same class, one per audience ────

    @Provides
    @Singleton
    @UserJwtVerifier
    public static JwtVerifier userJwtVerifier(Config config) {
        return new JwtVerifier(config.getString("wallet.keycloak.user.jwk-set-uri"));
    }

    @Provides
    @Singleton
    @AdminJwtVerifier
    public static JwtVerifier adminJwtVerifier(Config config) {
        return new JwtVerifier(config.getString("wallet.keycloak.admin.jwk-set-uri"));
    }

    @Provides
    @Singleton
    @BridgeJwtVerifier
    public static JwtVerifier bridgeJwtVerifier(Config config) {
        return new JwtVerifier(config.getString("wallet.keycloak.bridge.jwk-set-uri"));
    }

    // ── Jakarta Bean Validation (request-body DTOs) ───────────────────────────

    /** Default factory: collect-all (not failFast) so each violation maps to one error entry. */
    @Provides
    @Singleton
    public static Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ── services whose constructors carry config primitives (no @Inject) ──────

    /** Provided here (not {@code @Inject}) because of the {@code boolean activeCredit} config param. */
    @Provides
    @Singleton
    public static AdminWalletWebService adminWalletWebService(
            WalletQueryService walletQueryService,
            WalletService walletService,
            ir.ebb.external.rayan.wallet.service.query.RayanWalletQueryService rayanWalletQueryService,
            ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandService rayanWalletCommandService,
            CreditHistoryQueryService creditHistoryQueryService,
            CreditHistoryCommandService creditHistoryCommandService,
            @BlockingExecutor Executor blockingExecutor,
            Config config) {
        return new AdminWalletWebServiceImpl(walletQueryService, walletService, rayanWalletQueryService,
                rayanWalletCommandService, creditHistoryQueryService, creditHistoryCommandService,
                blockingExecutor, config.getBoolean("wallet.credit.active"));
    }

    /** Provided here (not {@code @Inject}) because of the topic-name config param. */
    @Provides
    @Singleton
    public static TurnoverNotifyWebService turnoverNotifyWebService(
            TurnoverQueryService turnoverQueryService,
            WalletService walletService,
            KafkaWalletProducer kafkaWalletProducer,
            Config config) {
        return new TurnoverNotifyWebServiceImpl(turnoverQueryService, walletService, kafkaWalletProducer,
                config.getString("wallet.kafka.topic.turnover-notification"));
    }

    /** Provided here (not {@code @Inject}) because of the {@code int port} config param. */
    @Provides
    @Singleton
    public static GrpcServer grpcServer(Config config,
                                        WalletGrpcServiceImpl walletGrpcService,
                                        BridgeGrpcAuthInterceptor authInterceptor) {
        return new GrpcServer(config.getInt("wallet.grpc.port"), walletGrpcService, authInterceptor);
    }

    // ── interface → impl bindings for @Inject-annotated impls ─────────────────

    @Binds
    abstract WalletWebService walletWebService(WalletWebServiceImpl impl);

    @Binds
    abstract TurnoverWebService turnoverWebService(TurnoverWebServiceImpl impl);

    @Binds
    abstract BridgeWalletWebService bridgeWalletWebService(BridgeWalletWebServiceImpl impl);

    @Binds
    abstract BridgeTurnoverWebService bridgeTurnoverWebService(BridgeTurnoverWebServiceImpl impl);

    @Binds
    abstract BidarDepositWalletWebService bidarDepositWalletWebService(BidarDepositWalletWebServiceImpl impl);

    @Binds
    abstract AdminWalletTransactionWebService adminWalletTransactionWebService(AdminWalletTransactionWebServiceImpl impl);

    @Binds
    abstract RayanWalletJobService rayanWalletJobService(RayanWalletJobServiceImpl impl);
}
