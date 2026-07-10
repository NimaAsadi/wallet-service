package ir.ebb.wallet.service.turnover.command;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.repository.turnover.TurnoverRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class TurnoverCommandServiceImpl implements TurnoverCommandService {

    private final TurnoverRepository turnoverRepository;

    @Override
    public void deleteAll() {
        turnoverRepository.deleteAll();
    }

    @Override
    public TurnoverEntity save(TurnoverEntity entity) {
        return turnoverRepository.save(entity);
    }

    @Override
    public List<TurnoverEntity> saveAll(List<TurnoverEntity> entities) {
        return turnoverRepository.saveAll(entities);
    }

    @Override
    public TurnoverEntity createDeposit(User user, UUID walletId, long amount, UUID trackingId, String receiptBankNumber) {
        return turnoverRepository.save(
                new TurnoverEntity(user, walletId, TurnoverOperationType.DEPOSIT, amount, trackingId, receiptBankNumber));
    }

    @Override
    public TurnoverEntity createWithdraw(User user, UUID walletId, long amount, UUID trackingId, Long rayanId) {
        return turnoverRepository.save(
                new TurnoverEntity(user, walletId, TurnoverOperationType.WITHDRAW, amount, trackingId, rayanId));
    }

    @Override
    public TurnoverEntity createTrade(User user, UUID walletId, TurnoverOperationType type,
                                      long debit, long credit, UUID trackingId,
                                      long quantity, long price, int tradeNumber,
                                      String isin, String companyName, String instrumentName) {
        return turnoverRepository.save(
                new TurnoverEntity(user, walletId, type, debit, credit, trackingId,
                        quantity, price, tradeNumber, isin, companyName, instrumentName));
    }

    @Override
    public void createRemaining(WalletEntity walletEntity) {
        long remaining = walletEntity.getT0().getBalance()
                + walletEntity.getT1().getBalance()
                + walletEntity.getT2().getBalance();
        turnoverRepository.save(
                new TurnoverEntity(
                        walletEntity.getUser(),
                        walletEntity.getId(),
                        TurnoverOperationType.REMAINING,
                        remaining,
                        UUID.randomUUID()
                )
        );
    }
}
