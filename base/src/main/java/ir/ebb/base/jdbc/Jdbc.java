package ir.ebb.base.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Replaces Spring's {@code @Transactional} and Spring Data JPA. Thin JDBC helper
 * backed by a {@link DataSource}: {@code inTx} wraps a commit/rollback boundary,
 * {@code withConn} is an auto-commit read/single-op, and the query/update helpers
 * keep repositories compact. SQL errors surface as {@link JdbcException} (runtime)
 * so repository methods stay checked-exception-free.
 */
public final class Jdbc {

    private Jdbc() {}

    @FunctionalInterface
    public interface TxAction<T> {
        T run(Connection conn) throws Exception;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Run {@code action} inside a single transaction (commit on success, rollback on failure). */
    public static <T> T inTx(DataSource ds, TxAction<T> action) {
        try (Connection conn = ds.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = action.run(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (Exception ignore) {
                    // best-effort; connection returns to pool on close
                }
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    /** Run {@code action} on an auto-commit connection (reads / single statements). */
    public static <T> T withConn(DataSource ds, TxAction<T> action) {
        try (Connection conn = ds.getConnection()) {
            return action.run(conn);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    // ── query / update helpers (SQLException wrapped in JdbcException) ────────

    public static <T> List<T> queryList(Connection conn, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new JdbcException(e);
        }
    }

    public static <T> Optional<T> queryOne(Connection conn, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new JdbcException(e);
        }
    }

    public static int update(Connection conn, String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new JdbcException(e);
        }
    }

    /** Execute {@code sql} once per parameter set as a single JDBC batch (one round-trip instead of N).
     *  Empty {@code paramSets} is a no-op. SQLException surfaces as {@link JdbcException}. */
    public static int[] batchUpdate(Connection conn, String sql, List<Object[]> paramSets) {
        if (paramSets.isEmpty()) {
            return new int[0];
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] params : paramSets) {
                bind(ps, params);
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw new JdbcException(e);
        }
    }

    public static long count(Connection conn, String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new JdbcException(e);
        }
    }

    /** Bind parameters, rendering enums as their name() (matching STRING enum columns). */
    public static void bind(PreparedStatement ps, Object... params) throws SQLException {
        int i = 1;
        for (Object p : params) {
            if (p == null) {
                ps.setObject(i++, null);
            } else if (p instanceof Enum<?> e) {
                ps.setObject(i++, e.name());
            } else {
                ps.setObject(i++, p);
            }
        }
    }

    // ── nullable ResultSet getters ────────────────────────────────────────────

    public static Long getLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    public static Integer getInteger(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static UUID getUuid(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        if (v == null) return null;
        return v instanceof UUID u ? u : UUID.fromString(v.toString());
    }

    public static LocalDateTime getDateTime(ResultSet rs, String col) throws SQLException {
        var ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toLocalDateTime();
    }
}
