package ir.ebb.wallet.app.admin.job;

import ir.ebb.external.rayan.wallet.service.command.RayanWalletHistoryCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteRayanWalletsHistoryJob {

    private final RayanWalletHistoryCommandService rayanWalletHistoryCommandService;

    @Scheduled(cron = "${wallet.rayan.remove.schedule}")
    public void init() {
        rayanWalletHistoryCommandService.deleteByDateBefore(14);
    }
}
