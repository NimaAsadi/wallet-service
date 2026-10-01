package ir.ebb.wallet.service.turnover.command;

import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.projection.entity.TurnoverEntity;
import ir.ebb.wallet.projection.entity.WalletEntity;


import java.util.List;
import java.util.UUID;

public interface TurnoverCommandService {

    void deleteAll();

    TurnoverEntity save(TurnoverEntity entity);

    List<TurnoverEntity> saveAll(List<TurnoverEntity> entities);

    TurnoverEntity createDeposit(Long accountNumber, UUID walletId, long amount, UUID trackingId, String receiptBankNumber);

    TurnoverEntity createWithdraw(Long accountNumber, UUID walletId, long amount, UUID trackingId, Long rayanId);

    TurnoverEntity createTrade(Long accountNumber, UUID walletId, TurnoverOperationType type,
                               long debit, long credit, UUID trackingId,
                               long quantity, long price, int tradeNumber,
                               String isin, String companyName, String instrumentName);

    void createRemaining(WalletEntity walletEntity);
}
