package ir.ebb.wallet.aggregate;

import io.vavr.control.Try;
import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.actor.command.CreateWallet;
import ir.ebb.wallet.actor.command.FreezeBalance;
import ir.ebb.wallet.actor.command.WalletCommand;
import ir.ebb.wallet.actor.event.BalanceFrozen;
import ir.ebb.wallet.actor.event.WalletCreated;
import ir.ebb.wallet.actor.event.WalletEvent;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.constant.valueobject.WalletParameter;
import ir.ebb.wallet.wallet.WalletSerializable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class WalletAggregate implements WalletSerializable {

    private final WalletParameter t0;
    private final WalletParameter t1;
    private final WalletParameter t2;
    private Long credit;
    private Long buyingPower;
    private Long initialCredit;
    private Long separCredit;
    private Long separInitialCredit;
    private final WalletDebt walletDebt;
    private final Long dbsAccountNumber;
    private final Set<UUID> trackingIds;

    public WalletAggregate(
            WalletParameter t0,
            WalletParameter t1,
            WalletParameter t2,
            Long credit,
            Long buyingPower,
            Long initialCredit,
            Long separCredit,
            Long separInitialCredit,
            WalletDebt walletDebt,
            Long dbsAccountNumber,
            Set<UUID> trackingIds
    ) {
        this.t0 = t0;
        this.t1 = t1;
        this.t2 = t2;
        this.credit = credit;
        this.buyingPower = buyingPower;
        this.initialCredit = initialCredit;
        this.separCredit = separCredit;
        this.separInitialCredit = separInitialCredit;
        this.walletDebt = walletDebt;
        this.dbsAccountNumber = dbsAccountNumber;
        this.trackingIds = trackingIds;
    }

    public static WalletCreated create(CreateWallet command) {
        var wallet =  new WalletAggregate(
                new WalletParameter(),
                new WalletParameter(),
                new WalletParameter(),
                0L,
                0L,
                0L,
                0L,
                0L,
                new WalletDebt(),
                command.dbsAccountNumber(),
                new HashSet<>()
        );
        return new WalletCreated(wallet);
    }

    public static WalletAggregate applyEvent(WalletCreated event) {
        return event.wallet();
    }

    public Try<WalletEvent> validate(WalletCommand command) {
        return Try.of(() -> {
           if (command instanceof FreezeBalance fb)
               return validate(fb);

           throw new BusinessException(ExceptionConstants.INVALID_COMMAND);
        });
    }

    public WalletAggregate applyEvent(WalletEvent event) {
        if (event instanceof BalanceFrozen bf)
            return applyEvent(bf);

        return this;
    }

    public BuyingPower buyingPower(SettlementDelay settlementDelay) {
        Long totalBalance = switch (settlementDelay) {
            case T_PLUS_2 -> t2.getBalance() + t1.getBalance() + t0.getBalance();
            case T_PLUS_1 -> t1.getBalance() + t0.getBalance();
            case T_PLUS_0 -> t0.getBalance();
        };
        return new BuyingPower(totalBalance, credit, separCredit);
    }

    public void withdraw(Money value, SettlementDelay settlementDelay) {
        WalletParameter walletParameter = getWalletParameter(settlementDelay);
        Long oldBalance = walletParameter.getBalance();
        Long newBalance = oldBalance - value.value();
        walletParameter.setBalance(newBalance);
    }

    public WalletParameter getWalletParameter(SettlementDelay settlementDelay) {
        return switch (settlementDelay) {
            case T_PLUS_0 -> t0;
            case T_PLUS_1 -> t1;
            case T_PLUS_2 -> t2;
        };
    }

    private BalanceFrozen validate(FreezeBalance command) {
        if (trackingIds.contains(command.trackingId()))
            throw new BusinessException(ExceptionConstants.DUPLICATE_TRACKING_ID);

        BuyingPower bp = buyingPower(command.settlementDelay());
        if (bp.sum(command.canSpendSeparCredit()) < command.value().value())
            throw new BusinessException(ExceptionConstants.INSUFFICIENT_BALANCE);

        return new BalanceFrozen(command.value(), command.settlementDelay(), command.walletTransactionType(), command.canSpendSeparCredit());
    }

    private WalletAggregate applyEvent(BalanceFrozen event) {
        WalletParameter walletParameter = getWalletParameter(event.settlementDelay());
        long oldFrozen = walletParameter.getFrozen();
        walletParameter.setFrozen(oldFrozen + event.value().value());

        long currentValue = event.value().value();
        var settlementDelay = event.settlementDelay();
        SettlementDelay lender = event.settlementDelay();
        while (currentValue > 0) {
            Long parameterBalance = walletParameter.getBalance();
            Long balance = Optional.ofNullable(parameterBalance).filter(b -> b >= 0).orElse(0L);
            long difference = Math.min(balance, currentValue);
            withdraw(new Money(difference), lender);
            currentValue -= difference;
            this.walletDebt.calculateDebt(settlementDelay, lender, difference);
            if (currentValue > 0) {
                lender = lender.getLender();
                if (lender != null) {
                    walletParameter = getWalletParameter(lender);
                } else {
                    spendCredit(currentValue);
                    currentValue = 0L;
                }
            }
        }

        return this;
    }

    private void spendCredit(long currentValue) {
        long minValue = Math.min(currentValue, credit);
        credit -= minValue;
        separCredit -= Math.abs(currentValue - minValue);
    }

}
