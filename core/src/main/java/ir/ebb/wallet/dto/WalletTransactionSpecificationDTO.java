package ir.ebb.wallet.dto;

import ir.ebb.wallet.constant.enumeration.WalletTransactionType;

import java.util.UUID;

public record WalletTransactionSpecificationDTO(
        String userId,
        Long dbsAccountNumber,
        WalletTransactionType type,
        UUID trackingCode
) {}
