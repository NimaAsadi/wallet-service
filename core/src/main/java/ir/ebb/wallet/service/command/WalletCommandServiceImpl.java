package ir.ebb.wallet.service.command;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.base.exception.OptimisticLockingFailureException;
import ir.ebb.base.jdbc.Jdbc;
import ir.ebb.base.jdbc.JdbcException;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class WalletCommandServiceImpl implements WalletCommandService {

    private final DataSource dataSource;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CreditHistoryRepository creditHistoryRepository;

    @Override
    public Wallet getWallet(User user) {
        return walletRepository.findFirstByUser(user)
                .map(WalletEntity::adaptToDomain)
                .orElseThrow(() -> new BusinessException(ExceptionConstants.WALLET_NOT_EXIST.getMessage(),
                        ExceptionConstants.WALLET_NOT_EXIST.getCode()));
    }

    @Override
    public void saveAll(List<Wallet> wallets) {
        wallets.forEach(this::save);
    }

    @Override
    public Wallet save(Wallet wallet) {
        Jdbc.inTx(dataSource, conn -> {
            applyMutation(conn, wallet);
            return null;
        });
        return wallet;
    }

    /** Version-checked wallet update + debt update + transaction inserts, on one connection. */
    private void applyMutation(Connection conn, Wallet wallet) {
        int updated = walletRepository.updateWalletNative(conn, wallet);
        if (updated == 0) {
            throw new OptimisticLockingFailureException(
                    "Concurrent wallet update detected for user: " + wallet.getUser());
        }
        walletRepository.updateWalletDebtNative(conn, wallet);
        walletTransactionRepository.saveAll(conn,
                wallet.getWalletTransactions().stream()
                        .map(tx -> {
                            var e = tx.adaptToEntity();
                            e.setWalletId(wallet.getId());
                            return e;
                        })
                        .toList());
    }

    @Override
    public void save(WalletEntity walletEntity) {
        if (walletEntity.getWalletDebtEntity() == null) {
            walletEntity.setWalletDebtEntity(new WalletDebtEntity(walletEntity));
        }
        Jdbc.inTx(dataSource, conn -> {
            walletRepository.insertWallet(conn, walletEntity);
            return null;
        });
    }

    @Override
    public void saveForCredit(Wallet wallet, CreditHistoryEntity creditHistoryEntity) {
        Jdbc.inTx(dataSource, conn -> {
            applyMutation(conn, wallet);
            creditHistoryRepository.save(conn, creditHistoryEntity);
            return null;
        });
    }

    @Override
    public Wallet getAndFreezeForT0(UUID trackingId, User user, long amount, WalletTransactionType type) throws ApplicationException {
        Wallet wallet = getWallet(user);
        wallet.freeze(trackingId, new Money(amount), SettlementDelay.T_PLUS_0, type, false);
        return save(wallet);
    }

    @Override
    public Wallet getAndUnfreezeForT0(UUID trackingId, User user, long amount, WalletTransactionType type) throws ApplicationException {
        Wallet wallet = getWallet(user);
        wallet.unfreeze(trackingId, new Money(amount), SettlementDelay.T_PLUS_0, type);
        return save(wallet);
    }

    @Override
    public Wallet getAndSpendT0(UUID trackingId, User user, long amount, WalletTransactionType type) throws ApplicationException {
        Wallet wallet = getWallet(user);
        wallet.spend(trackingId, new Money(amount), SettlementDelay.T_PLUS_0, type);
        return save(wallet);
    }

    @Override
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
    public void settleSeparCredits(Set<User> users) {
        walletRepository.settleSeparCreditsByUser(users);
    }

    /**
     * Atomically spends from one wallet and deposits to another in a single DB
     * transaction. Reads use the repository's own connection; writes share one
     * connection and the version-checked UPDATE guards against concurrent writes.
     */
    @Override
    public Wallet spendAndTransfer(UUID trackingId, User fromUser, User toUser, long amount,
                                   SettlementDelay settlementDelay, WalletTransactionType type) throws ApplicationException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Wallet fromWallet = loadWallet(fromUser);
                fromWallet.spend(trackingId, new Money(amount), settlementDelay, type);
                applyMutation(conn, fromWallet);

                Wallet toWallet = loadWallet(toUser);
                toWallet.deposit(trackingId, new Money(amount), settlementDelay, type);
                applyMutation(conn, toWallet);

                conn.commit();
                return fromWallet;
            } catch (ApplicationException e) {
                rollbackQuietly(conn);
                throw e;
            } catch (RuntimeException e) {
                rollbackQuietly(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new JdbcException(e);
        }
    }

    private Wallet loadWallet(User user) {
        return walletRepository.findFirstByUser(user)
                .map(WalletEntity::adaptToDomain)
                .orElseThrow(() -> new BusinessException(ExceptionConstants.WALLET_NOT_EXIST.getMessage(),
                        ExceptionConstants.WALLET_NOT_EXIST.getCode()));
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignore) {
            // best-effort
        }
    }
}
