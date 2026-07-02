package ir.ebb.wallet.app.admin.job;

import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.service.query.RayanWalletQueryService;
import ir.ebb.wallet.app.admin.service.RayanWalletJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetRayanWalletsJob {

    private final RayanWalletJobService rayanWalletJobService;
    private final RayanWalletQueryService rayanWalletQueryService;
    private final LockProvider lockProvider;

    private static final String LOCK_NAME = "RAYAN_WALLET_SCHEDULER";

    @Scheduled(cron = "${wallet.rayan.schedule}")
    public void init() {
        log.atInfo().log("***** Start update Rayan wallets job at {} ******", new Date());
        lockProvider.lock(lockConfig()).ifPresent(lock -> {
            try {
                Map<Long, RayanWalletDTO> allRayanWallets = rayanWalletQueryService.getAllWallets();
                rayanWalletJobService.updateFromRayan(allRayanWallets);
                rayanWalletJobService.syncWallets(allRayanWallets);
            } catch (ApplicationException e) {
                log.atError().log("Error updating wallets from Rayan: {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        });
    }

    @Scheduled(cron = "${wallet.rayan.backup.schedule}")
    public void backup() {
        lockProvider.lock(lockConfig()).ifPresent(lock -> {
            try {
                Map<Long, RayanWalletDTO> allRayanWallets = rayanWalletQueryService.getAllWallets();
                rayanWalletJobService.updateFromRayan(allRayanWallets);
                rayanWalletJobService.syncWallets(allRayanWallets);
            } catch (ApplicationException e) {
                log.atError().log("Error on backup update from Rayan: {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        });
    }

    private LockConfiguration lockConfig() {
        Duration lockDuration = Duration.of(55, ChronoUnit.MINUTES);
        return new LockConfiguration(Instant.now(), LOCK_NAME, lockDuration, lockDuration);
    }
}
