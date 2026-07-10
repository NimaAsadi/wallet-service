package ir.ebb.external.rayan.wallet.service.query;

import ir.ebb.base.constant.ApplicationConstants;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.external.rayan.configuration.RayanHttpClient;
import ir.ebb.external.rayan.configuration.RayanResult;
import ir.ebb.external.rayan.login.RayanLoginService;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.entity.RayanWalletEntity;
import ir.ebb.external.rayan.wallet.repository.RayanWalletRepository;
import ir.ebb.external.rayan.wallet.transformer.RayanWalletResponseTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class RayanWalletQueryServiceImpl implements RayanWalletQueryService {

    private final RayanLoginService rayanLoginService;
    private final RayanHttpClient rayanHttpClient;
    private final RayanWalletRepository rayanWalletRepository;

    @Override
    public Map<Long, RayanWalletDTO> getAllWallets() throws ApplicationException {
        return RayanResult.from(() -> rayanHttpClient.getAllWallets(
                        Integer.parseInt(ApplicationConstants.BROKERAGE_CODE),
                        rayanLoginService.getToken()))
                .map(RayanWalletResponseTransformer::toDtoMap)
                .getOrElseThrow();
    }

    @Override
    public RayanWalletDTO getUserWallet(Long dbsAccountNumber) throws ApplicationException {
        return rayanWalletRepository
                .findTopByAccountNumberOrderByUpdatedAt(dbsAccountNumber)
                .orElseThrow(() -> new ApplicationException("wallet not found"))
                .toDTO();
    }

    @Override
    public RayanWalletDTO findUserWallet(Long dbsAccountNumber) {
        return rayanWalletRepository
                .findTopByAccountNumberOrderByUpdatedAt(dbsAccountNumber)
                .orElse(new RayanWalletEntity(dbsAccountNumber))
                .toDTO();
    }
}
