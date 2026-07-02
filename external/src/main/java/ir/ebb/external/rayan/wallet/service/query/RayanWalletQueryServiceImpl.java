package ir.ebb.external.rayan.wallet.service.query;

import ir.ebb.base.constant.ApplicationConstants;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.external.rayan.configuration.RayanResult;
import ir.ebb.external.rayan.login.RayanLoginService;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import ir.ebb.external.rayan.wallet.entity.RayanWalletEntity;
import ir.ebb.external.rayan.wallet.gateway.RayanWalletGateway;
import ir.ebb.external.rayan.wallet.repository.RayanWalletRepository;
import ir.ebb.external.rayan.wallet.transformer.RayanWalletResponseTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RayanWalletQueryServiceImpl implements RayanWalletQueryService {

    private final RayanLoginService rayanLoginService;
    private final RayanWalletGateway rayanWalletGateway;
    private final RayanWalletRepository rayanWalletRepository;

    @Override
    @Retryable(
            noRetryFor = {BusinessException.class},
            retryFor = {Exception.class},
            maxAttemptsExpression = "${rayan.retry.max.attempts:3}",
            backoff = @Backoff(delayExpression = "${rayan.retry.delay:10000}"))
    public Map<Long, RayanWalletDTO> getAllWallets() throws ApplicationException {
        return RayanResult.from(() -> rayanWalletGateway.getAllWallets(
                        Integer.parseInt(ApplicationConstants.BROKERAGE_CODE),
                        rayanLoginService.getToken()
                ))
                .map(RayanWalletResponseTransformer::toDtoMap)
                .getOrElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public RayanWalletDTO getUserWallet(Long dbsAccountNumber) throws ApplicationException {
        return rayanWalletRepository
                .findTopByAccountNumberOrderByUpdatedAt(dbsAccountNumber)
                .orElseThrow(() -> new ApplicationException("wallet not found"))
                .toDTO();
    }

    @Override
    @Transactional(readOnly = true)
    public RayanWalletDTO findUserWallet(Long dbsAccountNumber) {
        return rayanWalletRepository
                .findTopByAccountNumberOrderByUpdatedAt(dbsAccountNumber)
                .orElse(new RayanWalletEntity(dbsAccountNumber))
                .toDTO();
    }
}
