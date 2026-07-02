package ir.ebb.wallet.app.bridge.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import lombok.Builder;

@Builder
public record BridgeTurnoverResponseDTO(
        String trackingId,
        Long createdAt,
        Long debit,
        Long credit,
        TurnoverOperationType type,
        Long tradedQuantity,
        Long tradedPrice,
        String isin,
        String issuingCompanyAfcName,
        String instrumentAfcNormName
) {}
