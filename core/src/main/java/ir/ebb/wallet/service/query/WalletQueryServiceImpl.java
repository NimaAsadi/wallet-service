package ir.ebb.wallet.service.query;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.projection.entity.WalletEntity;
import ir.ebb.wallet.projection.repository.WalletReadRepository;
import ir.ebb.wallet.service.BlockingExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WalletQueryServiceImpl implements WalletQueryService {

    private final WalletReadRepository walletReadRepository;
    private final @BlockingExecutor Executor blockingExecutor;

    private static BusinessException walletNotExist() {
        return new BusinessException(ExceptionConstants.WALLET_NOT_EXIST.getMessage(),
                ExceptionConstants.WALLET_NOT_EXIST.getCode());
    }

    private <T> CompletionStage<T> read(Supplier<T> blocking) {
        return CompletableFuture.supplyAsync(blocking, blockingExecutor);
    }

    @Override
    public CompletionStage<List<WalletEntity>> findAll() {
        return read(walletReadRepository::findAll);
    }

    @Override
    public CompletionStage<Page<WalletEntity>> findAll(WalletSpecificationDTO dto, PageRequest page) {
        return read(() -> walletReadRepository.findAll(dto, page));
    }

    @Override
    public CompletionStage<WalletEntity> getWalletEntity(Long dbsAccountNumber) {
        Objects.requireNonNull(dbsAccountNumber, "dbsAccountNumber");
        return read(() -> walletReadRepository.findByAccountNumber(dbsAccountNumber))
                .thenApply(existing -> existing.orElseThrow(WalletQueryServiceImpl::walletNotExist));
    }

    @Override
    public CompletionStage<Boolean> existsWallet(Long dbsAccountNumber) {
        Objects.requireNonNull(dbsAccountNumber, "dbsAccountNumber");
        return read(() -> walletReadRepository.findByUser_DbsAccountNumber(dbsAccountNumber).isPresent());
    }

    @Override
    public CompletionStage<List<WalletEntity>> getWalletEntities(WalletSpecificationDTO dto) {
        return read(() -> walletReadRepository.findAll(dto));
    }

    @Override
    public CompletionStage<List<WalletEntity>> getSeparCreditDebtorUsers() {
        return read(walletReadRepository::findSeparCreditDebtors);
    }

    @Override
    public CompletionStage<Void> checkSeparCreditDebt(Long accountNumber) {
        Objects.requireNonNull(accountNumber, "accountNumber");
        return read(() -> walletReadRepository.existsByUserAndSeparCreditLessThanSeparInitialCredit(accountNumber))
                .thenAccept(exists -> {
                    if (exists)
                        throw new BusinessException(ExceptionConstants.SEPAR_CREDIT_DEBT.getMessage(),
                                ExceptionConstants.SEPAR_CREDIT_DEBT.getCode());
                });
    }
}
