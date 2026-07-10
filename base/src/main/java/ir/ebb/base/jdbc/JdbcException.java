package ir.ebb.base.jdbc;

import java.sql.SQLException;

/**
 * Unchecked wrapper for {@link SQLException} thrown by the {@link Jdbc} helpers,
 * so repository methods don't propagate checked SQL exceptions (which would
 * complicate transaction lambdas). The cause is the original {@code SQLException}.
 */
public class JdbcException extends RuntimeException {
    public JdbcException(SQLException cause) {
        super(cause);
    }
    public JdbcException(String message, SQLException cause) {
        super(message, cause);
    }
}
