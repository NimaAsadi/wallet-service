package ir.ebb.wallet.app.bridge.transformer;

import ir.ebb.wallet.app.bridge.dto.response.BridgeTurnoverResponseDTO;
import ir.ebb.wallet.entity.TurnoverEntity;

import java.time.ZoneOffset;
import java.util.List;

public final class BridgeTurnoverTransformer {

    private BridgeTurnoverTransformer() {}

    public static BridgeTurnoverResponseDTO adapt(TurnoverEntity e) {
        return BridgeTurnoverResponseDTO.builder()
                .trackingId(e.getTrackingId() != null ? e.getTrackingId().toString() : null)
                .createdAt(e.getCreatedAt() != null
                        ? e.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli() : null)
                .debit(e.getDebit())
                .credit(e.getCredit())
                .type(e.getType())
                .tradedQuantity(e.getTradedQuantity())
                .tradedPrice(e.getTradedPrice())
                .isin(e.getIsin())
                .issuingCompanyAfcName(e.getIssuingCompanyAfcName())
                .instrumentAfcNormName(e.getInstrumentAfcNormName())
                .build();
    }

    public static List<BridgeTurnoverResponseDTO> adaptList(List<TurnoverEntity> entities) {
        return entities.stream()
                .filter(e -> e.getType() != ir.ebb.wallet.constant.enumeration.TurnoverOperationType.REMAINING)
                .map(BridgeTurnoverTransformer::adapt)
                .toList();
    }
}
