package ir.ebb.wallet.app.di;

import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import ir.ebb.wallet.app.infra.DataSourceProvider;
import ir.ebb.wallet.app.infra.LiquibaseMigrator;

import javax.inject.Singleton;
import javax.sql.DataSource;

@Module
public abstract class PersistenceModule {

    /**
     * Hikari pool + Liquibase migration as a provisioning side effect: Dagger fully completes a
     * {@code @Provides} method before the binding is injected anywhere, so no consumer of the
     * {@link DataSource} can ever observe an unmigrated schema. Main additionally requests this
     * binding first (before the ActorSystem) to preserve the historical startup order:
     * migrate → actor system → projections.
     */
    @Provides
    @Singleton
    public static DataSource dataSource(Config config) {
        DataSource dataSource = DataSourceProvider.hikari(
                config.getString("wallet.db.url"),
                config.getString("wallet.db.username"),
                config.getString("wallet.db.password"),
                config.getInt("wallet.db.pool-size"));
        LiquibaseMigrator.migrate(dataSource);
        return dataSource;
    }
}
