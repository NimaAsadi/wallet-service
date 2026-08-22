package ir.ebb.wallet.dto;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;

import java.util.UUID;

public record SpendDTO(
        Long dbsAccountNumber,
        UUID trackingId,
        Long amount,
        SettlementDelay settlementDelay,
        WalletTransactionType walletTransactionType,
        boolean canSpendSeparCredit
) {
}
