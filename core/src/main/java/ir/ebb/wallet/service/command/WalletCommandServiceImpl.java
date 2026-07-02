package ir.ebb.wallet.service.command;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import ir.ebb.wallet.entity.WalletDebtEntity;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.repository.WalletRepository;
import ir.ebb.wallet.repository.credit.CreditHistoryRepository;
import ir.ebb.wallet.repository.transaction.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletCommandServiceImpl implements WalletCommandService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CreditHistoryRepository creditHistoryRepository;

    @Override
    @Transactional
    public Wallet getWallet(User user) {
        return walletRepository.findFirstByUser(user)
                .map(WalletEntity::adaptToDomain)
                .orElseThrow(() -> new BusinessException(ExceptionConstants.WALLET_NOT_EXIST.getMessage(), ExceptionConstants.WALLET_NOT_EXIST.getCode()));
    }

    @Override
    @Transactional
    public void saveAll(List<Wallet> wallets) {
        wallets.forEach(this::save);
    }

    @Override
    @Transactional
    public Wallet save(Wallet wallet) {
        int updated = walletRepository.updateWalletNative(wallet);
        if (updated == 0) throw new OptimisticLockingFailureException("Concurrent wallet update detected for user: " + wallet.getUser());
        walletRepository.updateWalletDebtNative(wallet);
        walletTransactionRepository.saveAll(
                wallet.getWalletTransactions().stream()
                        .map(tx -> { var e = tx.adaptToEntity(); e.setWalletId(wallet.getId()); return e; })
                        .toList()
        );
        return wallet;
    }

    @Override
    @Transactional
    public void save(WalletEntity walletEntity) {
        if (walletEntity.getWalletDebtEntity() == null) {
            walletEntity.setWalletDebtEntity(new WalletDebtEntity(walletEntity));
        }
        walletRepository.save(walletEntity);
    }

    @Override
    @Transactional
    public void saveForCredit(Wallet wallet, CreditHistoryEntity creditHistoryEntity) {
        save(wallet);
        creditHistoryRepository.save(creditHistoryEntity);
    }

    @Override
    @Transactional
    public Wallet getAndFreezeForT0(UUID trackingId, User user, long amount, WalletTransactionType type) throws ApplicationException {
        Wallet wallet = getWallet(user);
        wallet.freeze(trackingId, new Money(amount), SettlementDelay.T_PLUS_0, type, false);
        return save(wallet);
    }

    @Override
    @Transactional
    public Wallet getAndUnfreezeForT0(UUID trackingId, User user, long amount, WalletTransactionType type) throws ApplicationException {
        Wallet wallet = getWallet(user);
        wallet.unfreeze(trackingId, new Money(amount), SettlementDelay.T_PLUS_0, type);
        return save(wallet);
    }

    @Override
    @Transactional
    public Wallet getAndSpendT0(UUID trackingId, User user, long amount, WalletTransactionType type) throws ApplicationException {
        Wallet wallet = getWallet(user);
        wallet.spend(trackingId, new Money(amount), SettlementDelay.T_PLUS_0, type);
        return save(wallet);
    }

    @Override
    @Transactional
    public void chargeSeparCredits(HashMap<User, Long> userAmounts, boolean isLoan) {
        userAmounts.forEach((user, amount) -> {
            try {
                Wallet wallet = getWallet(user);
                if (isLoan) {
                    wallet.setSeparInitialCredit(amount);
                    wallet.setSeparCredit(amount);
                } else {
                    wallet.decreaseSeparCredit(amount);
                }
                save(wallet);
            } catch (Exception e) {
                log.atWarn().log("Failed to charge separ credit for user {}: {}", user, e.getMessage());
            }
        });
    }

    @Override
    @Transactional
    public void settleSeparCredits(Set<User> users) {
        walletRepository.settleSeparCreditsByUser(users);
    }

    @Override
    @Transactional
    public Wallet spendAndTransfer(UUID trackingId, User fromUser, User toUser, long amount, SettlementDelay settlementDelay, WalletTransactionType type) throws ApplicationException {
        Wallet fromWallet = getWallet(fromUser);
        fromWallet.spend(trackingId, new Money(amount), settlementDelay, type);
        save(fromWallet);

        Wallet toWallet = getWallet(toUser);
        toWallet.deposit(trackingId, new Money(amount), settlementDelay, type);
        save(toWallet);

        return fromWallet;
    }
}
