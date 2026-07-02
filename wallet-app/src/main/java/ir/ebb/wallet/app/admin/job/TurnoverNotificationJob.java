package ir.ebb.wallet.app.admin.job;

import ir.ebb.wallet.app.admin.service.job.TurnoverNotifyWebService;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class TurnoverNotificationJob {

    private static final String LOCK_NAME = "TURNOVER_MESSAGE_SCHEDULER";

    private final LockProvider lockProvider;
    private final TurnoverNotifyWebService turnoverNotifyWebService;

    @Scheduled(cron = "${turnover.notify.schedule:0 0 20 * * *}")
    public void init() {
        log.atInfo().log("***** Start turnover notification job at {} ******", new Date());
        lockProvider.lock(lockConfig()).ifPresent(lock -> {
            try {
                turnoverNotifyWebService.aggregateUserTurnover();
            } catch (Exception e) {
                log.atError().log("Turnover notification job failed: ", e);
            } finally {
                lock.unlock();
            }
        });
        log.atInfo().log("***** End turnover notification job at {} ******", new Date());
    }

    private LockConfiguration lockConfig() {
        Duration lockDuration = Duration.of(60, ChronoUnit.MINUTES);
        return new LockConfiguration(Instant.now(), LOCK_NAME, lockDuration, lockDuration);
    }
}
