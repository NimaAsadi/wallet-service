package ir.ebb.wallet.app.bridge.dto.response;

import lombok.Builder;

@Builder
public record BridgeWalletResponseDTO(
        Long balance,
        Long credit,
        Long initCredit,
        Long t0,
        Long t1,
        Long t2,
        Long totalFrozen,
        Long separCredit,
        Long separInitCredit
) {}
