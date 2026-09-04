package ir.ebb.external.rayan.wallet.repository;

import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.external.rayan.wallet.entity.RayanWalletEntity;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RayanWalletRepository {

    private static final String SELECT = """
            SELECT id, version, account_number, national_code, customer_credit, financial_remain,
                   in_progress, bond, loan, sale_t0, sale_t1, sale_t2,
                   purchase_t0, purchase_t1, purchase_t2, created_at, updated_at
            FROM rayan_wallet
            """;

    private final DataSource dataSource;

    private static final Jdbc.RowMapper<RayanWalletEntity> MAPPER = rs -> {
        RayanWalletEntity e = new RayanWalletEntity();
        e.setId(Jdbc.getUuid(rs, "id"));
        e.setVersion(rs.getLong("version"));
        e.setAccountNumber(Jdbc.getLong(rs, "account_number"));
        e.setNationalCode(rs.getString("national_code"));
        e.setCustomerCredit(Jdbc.getLong(rs, "customer_credit"));
        e.setFinancialRemain(Jdbc.getLong(rs, "financial_remain"));
        e.setInProgress(Jdbc.getLong(rs, "in_progress"));
        e.setBond(Jdbc.getLong(rs, "bond"));
        e.setLoan(Jdbc.getLong(rs, "loan"));
        e.setSaleT0(Jdbc.getLong(rs, "sale_t0"));
        e.setSaleT1(Jdbc.getLong(rs, "sale_t1"));
        e.setSaleT2(Jdbc.getLong(rs, "sale_t2"));
        e.setPurchaseT0(Jdbc.getLong(rs, "purchase_t0"));
        e.setPurchaseT1(Jdbc.getLong(rs, "purchase_t1"));
        e.setPurchaseT2(Jdbc.getLong(rs, "purchase_t2"));
        e.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        e.setUpdatedAt(Jdbc.getDateTime(rs, "updated_at"));
        return e;
    };

    public Optional<RayanWalletEntity> findByAccountNumber(Long accountNumber) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryOne(conn, SELECT + " WHERE account_number = ?", MAPPER, accountNumber));
    }

    public Optional<RayanWalletEntity> findTopByAccountNumberOrderByUpdatedAt(Long accountNumber) {
        return Jdbc.withConn(dataSource, conn ->
                Jdbc.queryOne(conn, SELECT + " WHERE account_number = ? ORDER BY updated_at DESC LIMIT 1",
                        MAPPER, accountNumber));
    }

    public void deleteAll() {
        Jdbc.withConn(dataSource, conn -> Jdbc.update(conn, "DELETE FROM rayan_wallet WHERE 1=1"));
    }
}
