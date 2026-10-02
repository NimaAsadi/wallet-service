package ir.ebb.wallet.service.credit.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.projection.entity.CreditHistoryEntity;
import ir.ebb.wallet.projection.repository.CreditHistoryRepository;
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
public class CreditHistoryQueryServiceImpl implements CreditHistoryQueryService {

    private final CreditHistoryRepository creditHistoryRepository;
    private final @BlockingExecutor Executor blockingExecutor;

    @Override
    public CompletionStage<Page<CreditHistoryEntity>> findAll(CreditSpecificationDTO specificationDTO, PageRequest pageRequest) {
        return CompletableFuture.supplyAsync(
                () -> creditHistoryRepository.findAll(specificationDTO, pageRequest), blockingExecutor);
    }
}
