package ir.ebb.external.rayan.wallet.service.command;

import com.github.f4b6a3.uuid.UuidCreator;
import ir.ebb.base.constant.ApplicationConstants;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.external.rayan.configuration.RayanConfig;
import ir.ebb.external.rayan.login.RayanLoginService;
import ir.ebb.external.rayan.wallet.dto.RayanInitCreditResponseDTO;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.gateway.RayanWalletGateway;
import ir.ebb.external.rayan.wallet.repository.RayanWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RayanWalletCommandServiceImpl implements RayanWalletCommandService {

    private final RayanLoginService rayanLoginService;
    private final RayanWalletGateway rayanWalletGateway;
    private final RayanWalletRepository rayanWalletRepository;
    private final DataSource dataSource;

    @Override
    public void deleteAll() {
        log.atInfo().log("removing previous rayan wallets from DB");
        rayanWalletRepository.deleteAll();
        log.atInfo().log("previous rayan wallets removed from DB");
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void saveAll(Collection<RayanWalletDTO> rayanWalletDTOS) throws ApplicationException {
        log.atInfo().log("start save all of rayan wallets in DB");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            BaseConnection pgConn = connection.unwrap(BaseConnection.class);
            CopyManager copyManager = new CopyManager(pgConn);

            String sql = """
                    COPY rayan_wallet (
                        id, version, created_at, updated_at,
                        account_number, national_code,
                        customer_credit, financial_remain, in_progress,
                        bond, loan,
                        salet0, salet1, salet2,
                        purchaset0, purchaset1, purchaset2
                    ) FROM STDIN WITH (FORMAT csv)
                    """;

            copyManager.copyIn(sql, toCsvStream(rayanWalletDTOS));
            connection.commit();
            log.atInfo().log("end save all of rayan wallets in DB");
        } catch (SQLException | IOException e) {
            throw new ApplicationException("error on save rayan wallets in DB");
        }
    }

    @Override
    @Retryable(
            noRetryFor = {BusinessException.class},
            retryFor = {Exception.class},
            maxAttemptsExpression = "${rayan.retry.max.attempts:3}",
            backoff = @Backoff(delayExpression = "${rayan.retry.delay:10000}"))
    public RayanInitCreditResponseDTO initCredit(long credit, long dbsAccountNumber) {
        try {
            String token = rayanLoginService.getToken();
            String response = rayanWalletGateway.initCredit(
                    token, credit,
                    Integer.parseInt(ApplicationConstants.BROKERAGE_CODE),
                    dbsAccountNumber);
            log.atInfo().log(response);
        } catch (Exception e) {
            log.atError().log(e.getMessage());
            if (e instanceof HttpClientErrorException httpError && httpError.getStatusCode().is4xxClientError()) {
                if (httpError.getStatusCode().value() == 401) {
                    rayanLoginService.login();
                } else {
                    return new RayanInitCreditResponseDTO(false, e.getMessage());
                }
            }
            throw e;
        }
        return new RayanInitCreditResponseDTO(true, null);
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
