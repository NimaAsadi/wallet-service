package ir.ebb.wallet.service.turnover.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.projection.entity.TurnoverEntity;
import ir.ebb.wallet.projection.repository.TurnoverRepository;
import ir.ebb.wallet.service.BlockingExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TurnoverQueryServiceImpl implements TurnoverQueryService {

    private final TurnoverRepository turnoverRepository;
    private final @BlockingExecutor Executor blockingExecutor;

    private <T> CompletionStage<T> read(Supplier<T> blocking) {
        return CompletableFuture.supplyAsync(blocking, blockingExecutor);
    }

    @Override
    public CompletionStage<List<TurnoverEntity>> getTodayTurnoverByUser(long dbsAccountNumber) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        TurnoverSpecificationDTO spec = TurnoverSpecificationDTO.builder()
                .dbsAccountNumber(dbsAccountNumber)
                .fromCreatedAt(startOfDay)
                .toCreatedAt(endOfDay)
                .build();
        return read(() -> turnoverRepository.findAll(spec));
    }

    @Override
    public CompletionStage<Page<TurnoverEntity>> search(TurnoverSpecificationDTO spec, PageRequest pageRequest) {
        return read(() -> turnoverRepository.findAll(spec, pageRequest));
    }

    @Override
    public CompletionStage<List<Long>> getAccountNumberBatch(Long lastAccountNumber, int batchSize) {
        return read(() -> turnoverRepository.findAccountNumberBatch(lastAccountNumber, batchSize));
    }

    @Override
    public CompletionStage<List<TurnoverEntity>> getByAccountNumber(Long accountNumber) {
        return read(() -> turnoverRepository.findByAccountNumber(accountNumber));
    }
}
