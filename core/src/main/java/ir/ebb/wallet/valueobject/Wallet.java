package ir.ebb.wallet.valueobject;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import ir.ebb.wallet.entity.WalletDebtEntity;
import ir.ebb.wallet.entity.WalletEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@Data
public class Wallet {

    private UUID id;
    private Long version;
    private WalletParameter t0;
    private WalletParameter t1;
    private WalletParameter t2;
    private Long credit;
    private Long buyingPower;
    private Long initialCredit;
    private Long separCredit;
    private Long separInitialCredit;
    private WalletDebt walletDebt;
    private List<WalletTransaction> walletTransactions;
    private long accountNumber;

    public Wallet(long accountNumber) {
        this.accountNumber = accountNumber;
        this.t0 = new WalletParameter();
        this.t1 = new WalletParameter();
        this.t2 = new WalletParameter();
        this.credit = 0L;
        this.initialCredit = 0L;
        this.separCredit = 0L;
        this.separInitialCredit = 0L;
        this.walletDebt = new WalletDebt();
        this.walletTransactions = new ArrayList<>();
    }

    public Wallet() {}

    public void deposit(UUID trackingId, Money value, SettlementDelay settlementDelay, WalletTransactionType walletTransactionType) {
        WalletParameter walletParameter = getWalletParameter(settlementDelay);
        Long currentValue = value.value();

        long separCreditDiff = separInitialCredit - separCredit;
        if (separCreditDiff > 0) {
            if (separCreditDiff >= currentValue) {
                separCredit += currentValue;
                addWalletTransaction(currentValue, WalletOperationType.INCREASE_SEPAR_CREDIT, walletTransactionType, WalletParameterType.CREDIT, trackingId, walletParameter.getFrozen(), walletParameter.getFrozen(), walletParameter.getBalance(), walletParameter.getBalance());
                return;
            } else {
                separCredit += separCreditDiff;
                currentValue -= separCreditDiff;
            }
        }

        long creditDiff = initialCredit - credit;
        if (creditDiff > 0) {
            if (creditDiff >= currentValue) {
                credit += currentValue;
                addWalletTransaction(currentValue, WalletOperationType.INCREASE_CREDIT, walletTransactionType, WalletParameterType.CREDIT, trackingId, walletParameter.getFrozen(), walletParameter.getFrozen(), walletParameter.getBalance(), walletParameter.getBalance());
                return;
            } else {
                credit += creditDiff;
                currentValue -= creditDiff;
            }
        }

        settleDebt(trackingId, settlementDelay, walletParameter, currentValue, walletTransactionType);
    }



    public void withdraw(UUID trackingId, Money value, SettlementDelay settlementDelay, WalletTransactionType walletTransactionType) {
        WalletParameter walletParameter = getWalletParameter(settlementDelay);
        Long oldBalance = walletParameter.getBalance();
        Long newBalance = oldBalance - value.value();
        walletParameter.setBalance(newBalance);
        addWalletTransaction(value.value(), WalletOperationType.WITHDRAW, walletTransactionType, WalletParameterType.getBySettlementDelay(settlementDelay), trackingId, walletParameter.getFrozen(), walletParameter.getFrozen(), oldBalance, newBalance);
    }

    public void freeze(UUID trackingId, Money value, SettlementDelay settlementDelay, WalletTransactionType walletTransactionType, boolean canSpendSeparCredit) throws ApplicationException {
        Long currentValue = value.value();
        checkBuyingPower(settlementDelay, currentValue, canSpendSeparCredit);
        applyFreeze(trackingId, settlementDelay, walletTransactionType, currentValue);
    }

    public void freezeWithoutCredit(UUID trackingId, Money value, SettlementDelay settlementDelay, WalletTransactionType walletTransactionType) throws ApplicationException {
        Long currentValue = value.value();
        checkBalance(settlementDelay, currentValue);
        applyFreeze(trackingId, settlementDelay, walletTransactionType, currentValue);
    }

    public void checkBuyingPower(SettlementDelay settlementDelay, Long currentValue, boolean canSpendSeparCredit) throws ApplicationException {
        BuyingPower bp = buyingPower(settlementDelay);
        if (bp.sum(canSpendSeparCredit) < currentValue) {
            throw new ApplicationException(ExceptionConstants.INSUFFICIENT_BALANCE.getMessage());
        }
    }

    public void checkBalance(SettlementDelay settlementDelay, Long currentValue) throws ApplicationException {
        BuyingPower bp = buyingPower(settlementDelay);
        if (bp.balance() < currentValue) {
            throw new ApplicationException(ExceptionConstants.INSUFFICIENT_BALANCE.getMessage());
        }
    }

    public void spend(UUID trackingId, Money value, SettlementDelay settlementDelay, WalletTransactionType walletTransactionType) throws ApplicationException {
        Long currentValue = value.value();
        WalletParameter walletParameter = getWalletParameter(settlementDelay);
        Long currentFreeze = walletParameter.getFrozen();
        if (currentValue > currentFreeze) {
            throw new ApplicationException("Spend amount exceeds frozen balance");
        }
        Long newFrozen = currentFreeze - currentValue;
        walletParameter.setFrozen(newFrozen);
        addWalletTransaction(currentValue, WalletOperationType.SPEND, walletTransactionType, WalletParameterType.getBySettlementDelay(settlementDelay), trackingId, currentFreeze, newFrozen, walletParameter.getBalance(), walletParameter.getBalance());
        log.atInfo().log("Spend {} for trackingId#{} user#{} frozen: before#{} after#{}", currentValue, trackingId, accountNumber, currentFreeze, newFrozen);
    }

    public void unfreeze(UUID trackingId, Money value, SettlementDelay settlementDelay, WalletTransactionType walletTransactionType) throws ApplicationException {
        addWalletTransaction(value.value(), WalletOperationType.UNFREEZE, walletTransactionType, WalletParameterType.getBySettlementDelay(settlementDelay), trackingId, null, null, null, null);
        spend(trackingId, value, settlementDelay, walletTransactionType);
        deposit(trackingId, value, settlementDelay, walletTransactionType);
    }

    public void addCredit(UUID trackingId, Money value) {
        Long newCredit = value.value();
        Long oldCredit = initialCredit;
        if (Objects.equals(initialCredit, newCredit)) return;

        WalletOperationType walletOperationType;
        if (initialCredit > newCredit) {
            credit = credit - initialCredit + newCredit;
            walletOperationType = WalletOperationType.DECREASE_CREDIT;
            if (credit < 0) {
                withdraw(trackingId, new Money(Math.abs(credit)), SettlementDelay.T_PLUS_0, WalletTransactionType.ADMIN_CREDIT);
                credit = 0L;
            }
        } else {
            credit = newCredit - initialCredit + credit;
            walletOperationType = WalletOperationType.INCREASE_CREDIT;
        }
        initialCredit = newCredit;
        addWalletTransaction(Math.abs(oldCredit - newCredit), walletOperationType, WalletTransactionType.ADMIN_CREDIT, WalletParameterType.CREDIT, trackingId, null, null, oldCredit, credit);
    }

    public void decreaseSeparCredit(long value) {
        long debt = Math.min(separInitialCredit, value);
        separCredit -= debt;
        separInitialCredit -= debt;
    }

    public BuyingPower buyingPower(SettlementDelay settlementDelay) {
        Long totalBalance = switch (settlementDelay) {
            case T_PLUS_2 -> t2.getBalance() + t1.getBalance() + t0.getBalance();
            case T_PLUS_1 -> t1.getBalance() + t0.getBalance();
            case T_PLUS_0 -> t0.getBalance();
        };
        return new BuyingPower(totalBalance, credit, separCredit);
    }

    public WalletParameter getWalletParameter(SettlementDelay settlementDelay) {
        return switch (settlementDelay) {
            case T_PLUS_0 -> t0;
            case T_PLUS_1 -> t1;
            case T_PLUS_2 -> t2;
        };
    }

    public WalletEntity adaptToEntity() {
        WalletDebtEntity walletDebtEntity = walletDebt.adapt();
        WalletEntity walletEntity = WalletEntity.of(
                accountNumber,
                t0.adapt(), t1.adapt(), t2.adapt(),
                credit, initialCredit, separCredit, separInitialCredit,
                walletDebtEntity);
        walletDebtEntity.setId(id);
        walletEntity.setId(id);
        walletEntity.setVersion(version);
        return walletEntity;
    }

    public Long getTotalAsset() {
        return t0.getBalance() + t0.getFrozen()
                + t1.getBalance() + t1.getFrozen()
                + t2.getBalance() + t2.getFrozen();
    }

    private void settleDebt(UUID trackingId, SettlementDelay settlementDelay, WalletParameter walletParameter, Long currentValue, WalletTransactionType walletTransactionType) {
        switch (settlementDelay) {
            case T_PLUS_0 -> {
                Long oldBalance = walletParameter.getBalance();
                Long newBalance = oldBalance + currentValue;
                walletParameter.setBalance(newBalance);
                addWalletTransaction(currentValue, WalletOperationType.DEPOSIT, walletTransactionType, WalletParameterType.T0, trackingId, walletParameter.getFrozen(), walletParameter.getFrozen(), oldBalance, newBalance);
            }
            case T_PLUS_1 -> {
                Long debt = walletDebt.calculateDebtSettlement(settlementDelay, SettlementDelay.T_PLUS_0, currentValue);
                Long oldBalanceT0 = t0.getBalance();
                long newBalanceT0 = oldBalanceT0 + debt;
                t0.setBalance(newBalanceT0);
                addWalletTransaction(debt, WalletOperationType.DEPOSIT, walletTransactionType, WalletParameterType.T0, trackingId, t0.getFrozen(), t0.getFrozen(), oldBalanceT0, newBalanceT0);

                Long oldBalanceT1 = t1.getBalance();
                currentValue -= debt;
                long newBalanceT1 = oldBalanceT1 + currentValue;
                t1.setBalance(newBalanceT1);
                addWalletTransaction(currentValue, WalletOperationType.DEPOSIT, walletTransactionType, WalletParameterType.T1, trackingId, t1.getFrozen(), t1.getFrozen(), oldBalanceT1, newBalanceT1);
            }
            case T_PLUS_2 -> {
                Long debt = walletDebt.calculateDebtSettlement(settlementDelay, SettlementDelay.T_PLUS_0, currentValue);
                Long oldBalanceT0 = t0.getBalance();
                long newBalanceT0 = oldBalanceT0 + debt;
                t0.setBalance(newBalanceT0);
                addWalletTransaction(debt, WalletOperationType.DEPOSIT, walletTransactionType, WalletParameterType.T0, trackingId, t0.getFrozen(), t0.getFrozen(), oldBalanceT0, newBalanceT0);

                currentValue -= debt;
                debt = walletDebt.calculateDebtSettlement(settlementDelay, SettlementDelay.T_PLUS_1, currentValue);
                if (debt > 0) settleDebt(trackingId, SettlementDelay.T_PLUS_1, t0, debt, walletTransactionType);

                Long oldBalanceT2 = t2.getBalance();
                currentValue -= debt;
                long newBalanceT2 = t2.getBalance() + currentValue;
                t2.setBalance(newBalanceT2);
                addWalletTransaction(currentValue, WalletOperationType.DEPOSIT, walletTransactionType, WalletParameterType.T2, trackingId, t2.getFrozen(), t2.getFrozen(), oldBalanceT2, newBalanceT2);
            }
        }
    }

    private void applyFreeze(UUID trackingId, SettlementDelay settlementDelay, WalletTransactionType walletTransactionType, Long currentValue) throws ApplicationException {
        WalletParameter walletParameter = getWalletParameter(settlementDelay);
        long oldFrozen = walletParameter.getFrozen();
        walletParameter.setFrozen(oldFrozen + currentValue);
        addWalletTransaction(currentValue, WalletOperationType.FREEZE, walletTransactionType, WalletParameterType.getBySettlementDelay(settlementDelay), trackingId, oldFrozen, walletParameter.getFrozen(), walletParameter.getBalance(), walletParameter.getBalance());

        SettlementDelay lender = settlementDelay;
        while (currentValue > 0) {
            Long parameterBalance = walletParameter.getBalance();
            Long balance = Optional.ofNullable(parameterBalance).map(b -> b < 0 ? 0L : b).orElse(0L);
            long difference = Math.min(balance, currentValue);
            withdraw(trackingId, new Money(difference), lender, walletTransactionType);
            currentValue -= difference;
            this.walletDebt.calculateDebt(settlementDelay, lender, difference);
            if (currentValue > 0) {
                lender = lender.getLender();
                if (lender != null) {
                    walletParameter = getWalletParameter(lender);
                } else {
                    spendCredit(trackingId, currentValue, walletTransactionType);
                    currentValue = 0L;
                }
            }
        }
    }

    private void spendCredit(UUID trackingId, long currentValue, WalletTransactionType walletTransactionType) throws ApplicationException {
        checkSufficientCredit(currentValue);
        long oldCredit = credit;
        long minValue = Math.min(currentValue, credit);
        credit -= minValue;
        addWalletTransaction(currentValue, WalletOperationType.DECREASE_CREDIT, walletTransactionType, WalletParameterType.CREDIT, trackingId, null, null, oldCredit, credit);
        long oldSeparCredit = separCredit;
        separCredit -= Math.abs(currentValue - minValue);
        addWalletTransaction(currentValue, WalletOperationType.DECREASE_SEPAR_CREDIT, walletTransactionType, WalletParameterType.CREDIT, trackingId, null, null, oldSeparCredit, separCredit);
    }

    private void checkSufficientCredit(Long value) throws ApplicationException {
        if (credit + separCredit < value) {
            throw new ApplicationException(ExceptionConstants.INSUFFICIENT_BALANCE.getMessage());
        }
    }

    private void addWalletTransaction(Long amount, WalletOperationType walletOperationType,
                                      WalletTransactionType walletTransactionType, WalletParameterType walletParameterType,
                                      UUID trackingId, Long frozenBefore, Long frozenAfter,
                                      Long balanceBefore, Long balanceAfter) {
        if (amount == null || amount <= 0L) return;
        walletTransactions.add(WalletTransaction.builder()
                .walletId(id)
                .accountNumber(accountNumber)
                .walletOperationType(walletOperationType)
                .walletTransactionType(walletTransactionType)
                .walletParameterType(walletParameterType)
                .amount(amount).trackingId(trackingId)
                .frozenBefore(frozenBefore).frozenAfter(frozenAfter)
                .balanceBefore(balanceBefore).balanceAfter(balanceAfter)
                .build());
    }
}
