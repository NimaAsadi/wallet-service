package ir.ebb.userinfo.repository;

import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.common.model.user.User;
import ir.ebb.userinfo.entity.UserEntity;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.util.Optional;

@RequiredArgsConstructor
public class UserRepository {

    private static final String SELECT = """
            SELECT id, version, user_id, account_number, national_code, full_name,
                   account_name, father_name, mobile, email, created_at, updated_at
            FROM user_info
            """;

    private final DataSource dataSource;

    private static final Jdbc.RowMapper<UserEntity> MAPPER = rs -> {
        UserEntity e = new UserEntity();
        e.setUser(User.of(Jdbc.getUuid(rs, "user_id"), Jdbc.getLong(rs, "account_number")));
        e.setNationalCode(rs.getString("national_code"));
        e.setFullName(rs.getString("full_name"));
        e.setAccountName(rs.getString("account_name"));
        e.setFatherName(rs.getString("father_name"));
        e.setMobile(rs.getString("mobile"));
        e.setEmail(rs.getString("email"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));
        return e;
    };

    public Optional<UserEntity> findByUser(User user) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryOne(conn, SELECT + " WHERE user_id = ? AND account_number = ?",
                        MAPPER, user.getKeycloakId(), user.getDbsAccountNumber()));
    }

    public Optional<UserEntity> findByUserDbsAccountNumber(Long dbsAccountNumber) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryOne(conn, SELECT + " WHERE account_number = ?", MAPPER, dbsAccountNumber));
    }
}
