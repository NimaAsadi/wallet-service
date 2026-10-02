package ir.ebb.wallet.service.turnover.command;

import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.projection.entity.TurnoverEntity;
import ir.ebb.wallet.projection.entity.WalletEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface TurnoverCommandService {

    CompletionStage<Void> deleteAll();

    CompletionStage<TurnoverEntity> save(TurnoverEntity entity);

    CompletionStage<List<TurnoverEntity>> saveAll(List<TurnoverEntity> entities);

    CompletionStage<TurnoverEntity> createDeposit(Long accountNumber, UUID walletId, long amount, UUID trackingId, String receiptBankNumber);

    CompletionStage<TurnoverEntity> createWithdraw(Long accountNumber, UUID walletId, long amount, UUID trackingId, Long rayanId);

    CompletionStage<TurnoverEntity> createTrade(Long accountNumber, UUID walletId, TurnoverOperationType type,
                                                long debit, long credit, UUID trackingId,
                                                long quantity, long price, int tradeNumber,
                                                String isin, String companyName, String instrumentName);

    CompletionStage<Void> createRemaining(WalletEntity walletEntity);
}
