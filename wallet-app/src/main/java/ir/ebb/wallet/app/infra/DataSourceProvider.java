package ir.ebb.wallet.app.infra;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Builds the HikariCP {@link DataSource} from config (replaces Spring Boot's
 * auto-configured datasource).
 */
public final class DataSourceProvider {

    private DataSourceProvider() {}

    public static DataSource hikari(String url, String username, String password, int poolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(Math.max(2, poolSize / 8));
        config.setConnectionTimeout(3000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("wallet-hikari");
        return new HikariDataSource(config);
    }
}
