package ir.ebb.external.rayan.wallet.repository;

import ir.ebb.base.jdbc.Jdbc;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.time.LocalDateTime;

/**
 * JDBC replacement. The two batched deletes preserve the original 10k-chunk
 * native SQL; the caller loops until the returned count is 0.
 */
@RequiredArgsConstructor
public class RayanWalletHistoryRepository {

    private static final String DELETE_BEFORE = """
            DELETE FROM rayan_wallet_history WHERE id IN
            (SELECT id FROM rayan_wallet_history WHERE created_at < ? LIMIT 10000)
            """;

    private static final String DELETE_BETWEEN = """
            DELETE FROM rayan_wallet_history WHERE id IN
            (SELECT id FROM rayan_wallet_history WHERE created_at >= ? AND created_at < ? LIMIT 10000)
            """;

    private final DataSource dataSource;

    public long deleteByCreatedAtBefore(LocalDateTime date) {
        return Jdbc.withConn(dataSource, conn -> Jdbc.update(conn, DELETE_BEFORE, date));
    }

    public long deleteAllByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return Jdbc.withConn(dataSource, conn -> Jdbc.update(conn, DELETE_BETWEEN, startDate, endDate));
    }
}
