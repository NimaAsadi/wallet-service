package ir.ebb.wallet.app.user.dto.response;

import lombok.Builder;

@Builder
public record WalletResponseDTO(
        Long balance,
        Long credit,
        Long initCredit,
        Long separCredit,
        Long separInitCredit,
        Long t0,
        Long t1,
        Long t2,
        Long totalFrozen
) {}
