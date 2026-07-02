package ir.ebb.external.rayan.wallet.dto;

import lombok.Builder;

@Builder
public record RayanWalletDTO(
        Long accountNumber,
        String nationalCode,
        Long customerCredit,
        Long financialRemain,
        Long inProgress,
        Long bond,
        Long loan,
        Long saleT0,
        Long saleT1,
        Long saleT2,
        Long purchaseT0,
        Long purchaseT1,
        Long purchaseT2
) {}
