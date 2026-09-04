package ir.ebb.external.rayan.wallet.service.command;

import com.github.f4b6a3.uuid.UuidCreator;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.repository.RayanWalletHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RayanWalletHistoryCommandServiceImpl implements RayanWalletHistoryCommandService {

    private final RayanWalletHistoryRepository rayanWalletHistoryRepository;
    private final DataSource dataSource;

    @Override
    public void saveAll(Collection<RayanWalletDTO> rayanWalletDTOS) {
        log.atInfo().log("start save all of rayan wallets history in DB");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            BaseConnection pgConn = connection.unwrap(BaseConnection.class);
            CopyManager copyManager = new CopyManager(pgConn);

            String sql = """
                    COPY rayan_wallet_history (
                        id, version, created_at, updated_at,
                        account_number, national_code,
                        customer_credit, financial_remain, in_progress,
                        bond, loan,
                        sale_t0, sale_t1, sale_t2,
                        purchase_t0, purchase_t1, purchase_t2
                    ) FROM STDIN WITH (FORMAT csv)
                    """;

            copyManager.copyIn(sql, toCsvStream(rayanWalletDTOS));
            connection.commit();
            log.atInfo().log("end save all of rayan wallets history in DB");
        } catch (SQLException | IOException e) {
            log.atError().log("error on save rayan wallets history in DB", e);
        }
    }

    @Override
    public void deleteByDateBefore(int days) {
        log.atInfo().log("***** Start delete Rayan wallets history job at {} ******", new Date());
        long deletedRows;
        do {
            deletedRows = rayanWalletHistoryRepository
                    .deleteByCreatedAtBefore(LocalDate.now().atStartOfDay().minusDays(days));
        } while (deletedRows > 0);
        log.atInfo().log("***** End delete Rayan wallets history job ******");
    }

    @Override
    public void deleteTodayWallets() {
        log.atInfo().log("removing previous rayan wallets history from DB");
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long deletedRows;
        do {
            deletedRows = rayanWalletHistoryRepository
                    .deleteAllByCreatedAtBetween(startOfDay, startOfDay.plusDays(1));
        } while (deletedRows > 0);
        log.atInfo().log("previous rayan wallets history removed from DB");
    }

    private InputStream toCsvStream(Collection<RayanWalletDTO> dtos) {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in;
        try {
            in = new PipedInputStream(out, 64 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
                String now = LocalDateTime.now().toString();
                for (RayanWalletDTO dto : dtos) {
                    UUID id = UuidCreator.getTimeOrderedEpoch();
                    writer.append(id.toString()).append(',')
                            .append("0").append(',')
                            .append(now).append(',')
                            .append(now).append(',')
                            .append(nullSafe(dto.accountNumber())).append(',')
                            .append(nullSafe(dto.nationalCode())).append(',')
                            .append(nullSafe(dto.customerCredit())).append(',')
                            .append(nullSafe(dto.financialRemain())).append(',')
                            .append(nullSafe(dto.inProgress())).append(',')
                            .append(nullSafe(dto.bond())).append(',')
                            .append(nullSafe(dto.loan())).append(',')
                            .append(nullSafe(dto.saleT0())).append(',')
                            .append(nullSafe(dto.saleT1())).append(',')
                            .append(nullSafe(dto.saleT2())).append(',')
                            .append(nullSafe(dto.purchaseT0())).append(',')
                            .append(nullSafe(dto.purchaseT1())).append(',')
                            .append(nullSafe(dto.purchaseT2()))
                            .append('\n');
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return in;
    }

    private String nullSafe(Object o) {
        return o == null ? "" : o.toString();
    }
}
