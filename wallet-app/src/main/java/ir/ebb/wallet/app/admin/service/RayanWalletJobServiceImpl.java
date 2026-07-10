package ir.ebb.wallet.app.admin.service;

import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletCommandService;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletHistoryCommandService;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.app.admin.transformer.WalletTransformer;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.service.command.WalletCommandService;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.turnover.command.TurnoverCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RayanWalletJobServiceImpl implements RayanWalletJobService {

    private final WalletCommandService walletCommandService;
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
            Long accountNumber = wallet.getUser().getDbsAccountNumber();
            RayanWalletDTO rayanWallet = allRayanWallets.get(accountNumber);
            if (ObjectUtils.isEmpty(rayanWallet)) {
                log.atError().log("Rayan wallet not found for account={} userId={}",
                        accountNumber, wallet.getUser().getKeycloakId());
                return;
            }
            WalletTransformer.adapt(wallet, rayanWallet);
            turnovers.add(new TurnoverEntity(
                    wallet.getUser(), wallet.getId(),
                    TurnoverOperationType.REMAINING,
                    wallet.getTotalAsset(), wallet.getId()));
        });

        walletCommandService.saveAll(wallets);
        turnoverCommandService.saveAll(turnovers);
    }
}
