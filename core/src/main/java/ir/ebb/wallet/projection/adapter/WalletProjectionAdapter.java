package ir.ebb.wallet.projection.adapter;

import com.github.f4b6a3.uuid.UuidCreator;
import ir.ebb.wallet.aggregate.WalletAggregate;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import ir.ebb.wallet.projection.entity.WalletDebtEntity;
import ir.ebb.wallet.projection.entity.WalletEntity;
import ir.ebb.wallet.projection.entity.WalletTransactionEntity;
import ir.ebb.wallet.valueobject.Wallet;
import ir.ebb.wallet.valueobject.WalletDebt;
import ir.ebb.wallet.valueobject.WalletTransaction;

import java.util.Objects;
import java.util.UUID;

/**
 * Adapts the domain value objects ({@code ir.ebb.wallet.valueobject.*}) onto the projection
 * (read-model) entities ({@code ir.ebb.wallet.projection.entity.*}) that the generated R2DBC
 * repositories persist — the projection-family counterpart of the VOs' legacy
 * {@code adaptToEntity()}/{@code adapt()} methods targeting {@code ir.ebb.wallet.entity.*}.
 *
 * <p>Fields the VOs carry no source for arrive as parameters: {@code userId} (wallet +
 * transaction — the generated repositories bind nulls directly, which the Postgres R2DBC
 * driver rejects), the {@code wallet_debt} PK {@code walletId} (threaded in from the owning
 * wallet's id, like the legacy flow did), and the {@code wallet_transaction} {@code id}
 * (deterministic per journal event for replay idempotency — see the {@code WalletDbProjectionHandler}
 * contract). The four credit-related {@code WalletDebt} counters have no columns and are dropped.
 *
 * <p>Boxed {@code Long} sources unbox to the entities' primitive columns with a {@code 0}
 * default; the transaction's before/after audit columns stay boxed and pass {@code null}
 * through (meaningful for legs like unfreeze). {@code createdAt}/{@code updatedAt} are left
 * null — the schema defaults them on INSERT, and callers stamp {@code updatedAt} before
 * {@code updateOne(...)}.
 */
public final class WalletProjectionAdapter {

    private WalletProjectionAdapter() {
    }

    public static WalletEntity adapt(Wallet wallet) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        WalletEntity entity = new WalletEntity();
        entity.setId(wallet.getId());
        entity.setVersion(Objects.requireNonNullElse(wallet.getVersion(), 0L));
        entity.setAccountNumber(wallet.getAccountNumber());
        entity.setT0Balance(balance(wallet.getT0()));
        entity.setT0Frozen(frozen(wallet.getT0()));
        entity.setT1Balance(balance(wallet.getT1()));
        entity.setT1Frozen(frozen(wallet.getT1()));
        entity.setT2Balance(balance(wallet.getT2()));
        entity.setT2Frozen(frozen(wallet.getT2()));
        entity.setCredit(Objects.requireNonNullElse(wallet.getCredit(), 0L));
        entity.setInitialCredit(Objects.requireNonNullElse(wallet.getInitialCredit(), 0L));
        entity.setSeparCredit(Objects.requireNonNullElse(wallet.getSeparCredit(), 0L));
        entity.setSeparInitialCredit(Objects.requireNonNullElse(wallet.getSeparInitialCredit(), 0L));
        return entity;
    }

    public static WalletDebtEntity adapt(WalletDebt walletDebt, UUID walletId) {
        Objects.requireNonNull(walletDebt, "walletDebt must not be null");
        WalletDebtEntity entity = new WalletDebtEntity();
        entity.setWalletId(walletId);
        entity.setT2ToT0Debt(Objects.requireNonNullElse(walletDebt.getT2Tot0Debt(), 0L));
        entity.setT2ToT1Debt(Objects.requireNonNullElse(walletDebt.getT2Tot1Debt(), 0L));
        entity.setT1ToT0Debt(Objects.requireNonNullElse(walletDebt.getT1Tot0Debt(), 0L));
        return entity;
    }

    public static WalletTransactionEntity adapt(WalletTransaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        WalletTransactionEntity entity = new WalletTransactionEntity();
        entity.setId(UuidCreator.getTimeOrderedEpoch());
        entity.setAccountNumber(transaction.getAccountNumber());
        entity.setWalletId(transaction.getWalletId());
        entity.setWalletOperationType(transaction.getWalletOperationType());
        entity.setWalletTransactionType(transaction.getWalletTransactionType());
        entity.setWalletParameterType(transaction.getWalletParameterType());
        entity.setAmount(Objects.requireNonNullElse(transaction.getAmount(), 0L));
        entity.setTrackingId(transaction.getTrackingId());
        entity.setFrozenBefore(transaction.getFrozenBefore());
        entity.setFrozenAfter(transaction.getFrozenAfter());
        entity.setBalanceBefore(transaction.getBalanceBefore());
        entity.setBalanceAfter(transaction.getBalanceAfter());
        return entity;
    }

    public static Wallet adapt(WalletAggregate walletAggregate) {
        Objects.requireNonNull(walletAggregate, "walletAggregate must not be null");
        Wallet wallet = new Wallet();
        wallet.setId(walletAggregate.getId());
        wallet.setT0(walletAggregate.getT0());
        wallet.setT1(walletAggregate.getT1());
        wallet.setT2(walletAggregate.getT2());
        wallet.setCredit(walletAggregate.getCredit());
        wallet.setInitialCredit(walletAggregate.getInitialCredit());
        wallet.setSeparCredit(walletAggregate.getSeparCredit());
        wallet.setSeparInitialCredit(walletAggregate.getSeparInitialCredit());
        wallet.setWalletDebt(walletAggregate.getWalletDebt());
        wallet.setAccountNumber(wallet.getAccountNumber());
        return wallet;
    }

    private static long balance(WalletParameter parameter) {
        return parameter == null ? 0L : Objects.requireNonNullElse(parameter.getBalance(), 0L);
    }

    private static long frozen(WalletParameter parameter) {
        return parameter == null ? 0L : Objects.requireNonNullElse(parameter.getFrozen(), 0L);
    }

    public static Wallet adapt(WalletEntity entity, WalletDebtEntity debtEntity) {
        var wallet = new Wallet();
        wallet.setId(entity.getId());
        wallet.setT0(new WalletParameter(entity.getT0Balance(), entity.getT0Frozen()));
        wallet.setT1(new WalletParameter(entity.getT1Balance(), entity.getT1Frozen()));
        wallet.setT2(new WalletParameter(entity.getT2Balance(), entity.getT2Frozen()));
        wallet.setInitialCredit(entity.getInitialCredit());
        wallet.setCredit(entity.getCredit());
        wallet.setSeparInitialCredit(entity.getSeparInitialCredit());
        wallet.setSeparCredit(entity.getSeparCredit());

        var walletDebt = new WalletDebt();
        walletDebt.setT2Tot0Debt(debtEntity.getT2ToT0Debt());
        walletDebt.setT2Tot1Debt(debtEntity.getT2ToT1Debt());
        walletDebt.setT1Tot0Debt(debtEntity.getT1ToT0Debt());
        walletDebt.setT2ToCreditDebt(debtEntity.getT2ToCreditDebt());
        walletDebt.setT1ToCreditDebt(debtEntity.getT1ToCreditDebt());
        walletDebt.setT2ToSeparCreditDebt(debtEntity.getT2ToSeparCreditDebt());
        walletDebt.setT1ToSeparCreditDebt(debtEntity.getT1ToSeparCreditDebt());

        wallet.setWalletDebt(walletDebt);
        wallet.setAccountNumber(entity.getAccountNumber());
        return wallet;
    }
}
