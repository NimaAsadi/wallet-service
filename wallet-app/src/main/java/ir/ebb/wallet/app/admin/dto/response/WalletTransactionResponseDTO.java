package ir.ebb.wallet.app.admin.dto.response;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;

public record WalletTransactionResponseDTO(
        WalletTransactionType type,
        User user,
        WalletOperationType operationType,
        WalletParameterType walletParameterType,
        Long amount
) {}
