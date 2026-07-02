package ir.ebb.wallet.service.command;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import ir.ebb.wallet.entity.WalletEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface WalletCommandService {

    Wallet getWallet(User user);

    void saveAll(List<Wallet> wallets);

    Wallet save(Wallet wallet);

    void save(WalletEntity walletEntity);

    void saveForCredit(Wallet wallet, CreditHistoryEntity creditHistoryEntity);

    Wallet getAndFreezeForT0(UUID trackingId, User user, long amount, ir.ebb.wallet.constant.enumeration.WalletTransactionType type) throws ApplicationException;

    Wallet getAndUnfreezeForT0(UUID trackingId, User user, long amount, ir.ebb.wallet.constant.enumeration.WalletTransactionType type) throws ApplicationException;

    Wallet getAndSpendT0(UUID trackingId, User user, long amount, ir.ebb.wallet.constant.enumeration.WalletTransactionType type) throws ApplicationException;

    void chargeSeparCredits(HashMap<User, Long> userAmounts, boolean isLoan);

    void settleSeparCredits(Set<User> users);

    Wallet spendAndTransfer(UUID trackingId, User fromUser, User toUser, long amount, SettlementDelay settlementDelay, ir.ebb.wallet.constant.enumeration.WalletTransactionType type) throws ApplicationException;
}
