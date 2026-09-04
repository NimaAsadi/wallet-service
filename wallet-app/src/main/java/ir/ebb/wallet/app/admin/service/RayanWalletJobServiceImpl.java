package ir.ebb.wallet.app.admin.service;

import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandService;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletHistoryCommandService;
import ir.ebb.wallet.valueobject.Wallet;
import ir.ebb.wallet.app.admin.transformer.WalletTransformer;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.turnover.command.TurnoverCommandService;
import ir.ebb.wallet.wallet.WalletFacade;
import ir.ebb.wallet.wallet.WalletState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RayanWalletJobServiceImpl implements RayanWalletJobService {

    private final WalletFacade walletFacade;
    private final WalletQueryService walletQueryService;
    private final RayanWalletCommandService rayanWalletCommandService;
    private final RayanWalletHistoryCommandService rayanWalletHistoryCommandService;
    private final TurnoverCommandService turnoverCommandService;

    @Override
    public void updateFromRayan(Map<Long, RayanWalletDTO> allRayanWallets) throws ApplicationException {
        rayanWalletCommandService.deleteAll();
        rayanWalletCommandService.saveAll(allRayanWallets.values());
        rayanWalletHistoryCommandService.deleteTodayWallets();
        rayanWalletHistoryCommandService.saveAll(allRayanWallets.values());
    }

    @Override
    public void syncWallets(Map<Long, RayanWalletDTO> allRayanWallets) {
        List<Wallet> wallets = walletQueryService.findAll().parallelStream()
                .map(entity -> {
                    if (ObjectUtils.isNotEmpty(entity.getWalletDebtEntity())) {
                        entity.getWalletDebtEntity().clear();
                    }
                    return entity.adaptToDomain();
                })
                .collect(Collectors.toList());

        List<TurnoverEntity> turnovers = Collections.synchronizedList(new ArrayList<>());

        wallets.parallelStream().forEach(wallet -> {
            Long accountNumber = wallet.getAccountNumber();
            RayanWalletDTO rayanWallet = allRayanWallets.get(accountNumber);
            if (ObjectUtils.isEmpty(rayanWallet)) {
                log.atError().log("Rayan wallet not found for account={}", accountNumber);
                return;
            }
            WalletTransformer.adapt(wallet, rayanWallet);
            // Reconcile the sharded, event-sourced wallet toward the authoritative Rayan snapshot
            // (fire-and-forget tell; the entity applies WalletMutated on top of its current state).
            walletFacade.reconcileFromRayan(WalletState.fromAggregate(wallet, List.of()));
            turnovers.add(new TurnoverEntity(
                    null, wallet.getId(),
                    TurnoverOperationType.REMAINING,
                    wallet.getTotalAsset(), wallet.getId()));
        });

        turnoverCommandService.saveAll(turnovers);
    }
}
