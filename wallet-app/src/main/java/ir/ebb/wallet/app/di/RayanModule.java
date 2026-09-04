package ir.ebb.wallet.app.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
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
import org.apache.pekko.actor.typed.ActorSystem;

import javax.inject.Singleton;

/** Rayan gateway integration (external module). */
@Module
public abstract class RayanModule {

    @Provides
    @Singleton
    public static RayanHttpClient rayanHttpClient(ActorSystem<?> actorSystem, ObjectMapper objectMapper, Config config) {
        return new RayanHttpClient(actorSystem, config.getString("wallet.rayan.base-url"), objectMapper);
    }

    /**
     * {@link RayanResult} is static global state that must be configured before any Rayan
     * call is decoded. Doing it here is provably safe: every Rayan service
     * constructor-depends on this exact binding (its only {@code RayanResult.from} caller is
     * {@code RayanWalletQueryServiceImpl}), so {@code configure} precedes all of them by
     * construction.
     */
    @Provides
    @Singleton
    public static RayanLoginService rayanLoginService(RayanHttpClient rayanHttpClient, ObjectMapper objectMapper, Config config) {
        RayanLoginService rayanLoginService = new RayanLoginServiceImpl(
                rayanHttpClient,
                config.getString("wallet.rayan.auth.username"),
                config.getString("wallet.rayan.auth.password"),
                config.getString("wallet.rayan.auth.application-key"));
        RayanResult.configure(objectMapper, rayanLoginService);
        return rayanLoginService;
    }

    @Binds
    abstract RayanWalletQueryService rayanWalletQueryService(RayanWalletQueryServiceImpl impl);

    @Binds
    abstract RayanWalletCommandService rayanWalletCommandService(RayanWalletCommandServiceImpl impl);

    @Binds
    abstract RayanWalletHistoryCommandService rayanWalletHistoryCommandService(RayanWalletHistoryCommandServiceImpl impl);
}
