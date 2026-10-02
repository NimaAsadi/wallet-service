package ir.ebb.wallet.service.credit.command;

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
public class CreditHistoryCommandServiceImpl implements CreditHistoryCommandService {

    private final CreditHistoryRepository creditHistoryRepository;
    private final @BlockingExecutor Executor blockingExecutor;

    @Override
    public CompletionStage<Void> save(CreditHistoryEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            creditHistoryRepository.save(entity);
            return null;
        }, blockingExecutor);
    }
}
