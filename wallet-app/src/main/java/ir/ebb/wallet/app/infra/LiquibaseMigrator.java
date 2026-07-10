package ir.ebb.wallet.app.infra;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Runs Liquibase migrations programmatically at startup (replaces Spring Boot's
 * Liquibase auto-configuration). Changelog: {@code db/changelog/db.changelog-master.xml}.
 */
public final class LiquibaseMigrator {

    private LiquibaseMigrator() {}

    public static void migrate(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            try (Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database)) {
                liquibase.update();
            }
        } catch (Exception e) {
            throw new RuntimeException("Liquibase migration failed", e);
        }
    }
}
