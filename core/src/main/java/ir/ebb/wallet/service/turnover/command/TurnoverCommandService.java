package ir.ebb.wallet.service.turnover.command;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.entity.WalletEntity;

import java.util.List;
import java.util.UUID;

public interface TurnoverCommandService {

    void deleteAll();

    TurnoverEntity save(TurnoverEntity entity);

    List<TurnoverEntity> saveAll(List<TurnoverEntity> entities);

    TurnoverEntity createDeposit(User user, UUID walletId, long amount, UUID trackingId, String receiptBankNumber);

    TurnoverEntity createWithdraw(User user, UUID walletId, long amount, UUID trackingId, Long rayanId);

    TurnoverEntity createTrade(User user, UUID walletId, TurnoverOperationType type,
                               long debit, long credit, UUID trackingId,
                               long quantity, long price, int tradeNumber,
                               String isin, String companyName, String instrumentName);

    void createRemaining(WalletEntity walletEntity);
}
