package ir.ebb.wallet.service.transaction.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.projection.entity.WalletTransactionEntity;
import ir.ebb.wallet.projection.repository.WalletTransactionReadRepository;
import ir.ebb.wallet.service.BlockingExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WalletTransactionQueryServiceImpl implements WalletTransactionQueryService {

    private final WalletTransactionReadRepository walletTransactionReadRepository;
    private final @BlockingExecutor Executor blockingExecutor;

    @Override
    public CompletionStage<Page<WalletTransactionEntity>> findAll(WalletTransactionSpecificationDTO spec, PageRequest pageRequest) {
        return CompletableFuture.supplyAsync(
                () -> walletTransactionReadRepository.findAll(spec, pageRequest), blockingExecutor);
    }
}
