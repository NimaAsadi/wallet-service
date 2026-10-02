package ir.ebb.wallet.app.admin.service;

import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandService;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletHistoryCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Refreshes the {@code rayan_wallet} / {@code rayan_wallet_history} cache tables from the
 * Rayan snapshot. The legacy entity-reconcile (pushing Rayan balances into the sharded wallets
 * + derived turnover rows) is gone — wallet state is event-sourced and the read model is fed
 * exclusively by the projection.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RayanWalletJobServiceImpl implements RayanWalletJobService {

    private final RayanWalletCommandService rayanWalletCommandService;
    private final RayanWalletHistoryCommandService rayanWalletHistoryCommandService;

    @Override
    public void updateFromRayan(Map<Long, RayanWalletDTO> allRayanWallets) throws ApplicationException {
        rayanWalletCommandService.deleteAll();
        rayanWalletCommandService.saveAll(allRayanWallets.values());
        rayanWalletHistoryCommandService.deleteTodayWallets();
        rayanWalletHistoryCommandService.saveAll(allRayanWallets.values());
    }
}
