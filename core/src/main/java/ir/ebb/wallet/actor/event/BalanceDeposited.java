package ir.ebb.wallet.actor.event;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.Money;

import java.util.UUID;

public record BalanceDeposited(
        UUID trackingId,
        Money value,
        SettlementDelay settlementDelay,
        WalletTransactionType walletTransactionType,
        Long dbsAccountNumber
) implements WalletEvent {
}
