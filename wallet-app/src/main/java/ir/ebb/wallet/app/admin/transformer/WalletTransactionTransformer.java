package ir.ebb.wallet.app.admin.transformer;

import ir.ebb.wallet.app.admin.dto.request.WalletTransactionSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletTransactionResponseDTO;
import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.entity.WalletTransactionEntity;

public final class WalletTransactionTransformer {

    private WalletTransactionTransformer() {}

    public static WalletTransactionSpecificationDTO adapt(WalletTransactionSearchRequestDTO req) {
        return new WalletTransactionSpecificationDTO(
                req.getUserId(),
                req.getAccountNumber(),
                req.getType(),
                req.getTrackingCode()
        );
    }

    public static WalletTransactionResponseDTO adapt(WalletTransactionEntity e) {
        return new WalletTransactionResponseDTO(
                e.getWalletTransactionType(),
                null,
                e.getWalletOperationType(),
                e.getWalletParameterType(),
                e.getAmount()
        );
    }
}
