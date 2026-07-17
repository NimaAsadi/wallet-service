package ir.ebb.wallet.infrastructure.projection;

import org.apache.pekko.japi.function.Function;
import org.apache.pekko.projection.jdbc.JdbcSession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pekko Projection {@link JdbcSession} backed by a single HikariCP connection. A fresh
 * instance is created per envelope by the projection's session factory. Auto-commit is off
 * so the projection framework commits the offset (and, for {@code exactlyOnce}, the
 * handler's read-model writes) in one transaction.
 */
public final class HikariJdbcSession implements JdbcSession {

    private final Connection connection;

    public HikariJdbcSession(DataSource dataSource) {
        try {
            this.connection = dataSource.getConnection();
            this.connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to open projection JdbcSession", e);
        }
    }

    @Override
    public <R> R withConnection(Function<Connection, R> block) throws Exception {
        return block.apply(connection);
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        connection.rollback();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
