package ir.ebb.wallet.app.admin.dto.response;

import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;

import java.util.UUID;

public record WalletTransactionResponseDTO(
        WalletTransactionType type,
        UUID userId,
        WalletOperationType operationType,
        WalletParameterType walletParameterType,
        Long amount
) {}
