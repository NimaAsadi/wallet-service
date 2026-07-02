package ir.ebb.wallet.app.admin.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record WalletResponseDTO(
        UUID userId,
        Long dbsAccountNumber,
        Long t0Balance,
        Long t0BuyingPower,
        Long t1Balance,
        Long t1BuyingPower,
        Long t2Balance,
        Long t2BuyingPower,
        Long initialCredit,
        Long credit,
        Long creditUsage,
        Long separInitialCredit,
        Long separCredit,
        Long separCreditUsage,
        Long frozen
) {}
