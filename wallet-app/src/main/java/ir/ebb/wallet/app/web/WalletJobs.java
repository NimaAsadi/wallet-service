package ir.ebb.wallet.app.web;

import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandService;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletHistoryCommandService;
import ir.ebb.external.rayan.wallet.service.query.RayanWalletQueryService;
import ir.ebb.wallet.app.admin.service.RayanWalletJobService;
import ir.ebb.wallet.app.admin.service.job.TurnoverNotifyWebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Wires the four scheduled jobs (Rayan sync + backup, history cleanup, turnover
 * notification) onto the Pekko {@link CronScheduler}. Replaces the Spring
 * {@code @Scheduled} + ShedLock job classes.
 */
@Slf4j
@RequiredArgsConstructor
public class WalletJobs {

    private final CronScheduler scheduler;
    private final RayanWalletQueryService rayanWalletQueryService;
    private final RayanWalletJobService rayanWalletJobService;
    private final RayanWalletHistoryCommandService rayanWalletHistoryCommandService;
    private final TurnoverNotifyWebService turnoverNotifyWebService;

    private final String rayanSchedule;
    private final String rayanBackupSchedule;
    private final String rayanRemoveSchedule;
    private final String turnoverNotifySchedule;

    public void scheduleAll() {
        scheduler.schedule("rayan-wallets", rayanSchedule, this::rayanSync);
        scheduler.schedule("rayan-wallets-backup", rayanBackupSchedule, this::rayanSync);
        scheduler.schedule("rayan-wallets-remove", rayanRemoveSchedule, this::rayanRemove);
        scheduler.schedule("turnover-notify", turnoverNotifySchedule, turnoverNotifyWebService::aggregateUserTurnover);
    }

    private void rayanSync() {
        try {
            Map<Long, RayanWalletDTO> all = rayanWalletQueryService.getAllWallets();
            rayanWalletJobService.updateFromRayan(all);
            rayanWalletJobService.syncWallets(all);
        } catch (ir.ebb.common.exception.handler.ApplicationException e) {
            log.error("Rayan sync job failed", e);
        }
    }

    private void rayanRemove() {
        rayanWalletHistoryCommandService.deleteByDateBefore(14);
    }
}
