package ir.ebb.wallet.service.turnover.command;

import com.github.f4b6a3.uuid.UuidCreator;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.projection.entity.TurnoverEntity;
import ir.ebb.wallet.projection.entity.WalletEntity;
import ir.ebb.wallet.projection.repository.TurnoverRepository;
import ir.ebb.wallet.service.BlockingExecutor;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TurnoverCommandServiceImpl implements TurnoverCommandService {

    private final TurnoverRepository turnoverRepository;
    private final @BlockingExecutor Executor blockingExecutor;

    @Override
    public CompletionStage<Void> deleteAll() {
        return CompletableFuture.supplyAsync(() -> {
            turnoverRepository.deleteAll();
            return null;
        }, blockingExecutor);
    }

    @Override
    public CompletionStage<TurnoverEntity> save(TurnoverEntity entity) {
        return CompletableFuture.supplyAsync(() -> turnoverRepository.save(entity), blockingExecutor);
    }

    @Override
    public CompletionStage<List<TurnoverEntity>> saveAll(List<TurnoverEntity> entities) {
        return CompletableFuture.supplyAsync(() -> turnoverRepository.saveAll(entities), blockingExecutor);
    }

    @Override
    public CompletionStage<TurnoverEntity> createDeposit(Long accountNumber, UUID walletId, long amount, UUID trackingId, String receiptBankNumber) {
        return save(new TurnoverEntity(accountNumber, walletId, TurnoverOperationType.DEPOSIT, amount, trackingId, receiptBankNumber));
    }

    @Override
    public CompletionStage<TurnoverEntity> createWithdraw(Long accountNumber, UUID walletId, long amount, UUID trackingId, Long rayanId) {
        return save(new TurnoverEntity(accountNumber, walletId, TurnoverOperationType.WITHDRAW, amount, trackingId, rayanId));
    }

    @Override
    public CompletionStage<TurnoverEntity> createTrade(Long accountNumber, UUID walletId, TurnoverOperationType type,
                                                       long debit, long credit, UUID trackingId,
                                                       long quantity, long price, int tradeNumber,
                                                       String isin, String companyName, String instrumentName) {
        return save(new TurnoverEntity(accountNumber, walletId, type, debit, credit, trackingId,
                quantity, price, tradeNumber, isin, companyName, instrumentName));
    }

    @Override
    public CompletionStage<Void> createRemaining(WalletEntity walletEntity) {
        long remaining = walletEntity.getT0Balance() + walletEntity.getT1Balance() + walletEntity.getT2Balance();
        // The INSERT binds id (no schema default) — mint it here.
        TurnoverEntity entity = new TurnoverEntity(walletEntity.getAccountNumber(), walletEntity.getId(),
                TurnoverOperationType.REMAINING, remaining, UuidCreator.getTimeOrderedEpoch());
        entity.setId(UuidCreator.getTimeOrderedEpoch());
        return save(entity).thenApply(__ -> null);
    }
}
