package ir.ebb.wallet.app.user.transformer;

import ir.ebb.wallet.app.user.dto.response.TurnoverResponseDTO;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import ir.ebb.wallet.entity.TurnoverEntity;

import java.util.List;

public final class TurnoverTransformer {

    private TurnoverTransformer() {}

    public static TurnoverResponseDTO adapt(TurnoverEntity entity) {
        return TurnoverResponseDTO.builder()
                .trackingId(entity.getTrackingId() != null ? entity.getTrackingId().toString() : null)
                .createdAt(entity.getCreatedAt() != null
                        ? entity.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 : null)
                .debit(entity.getDebit())
                .credit(entity.getCredit())
                .type(entity.getType())
                .tradedQuantity(entity.getTradedQuantity())
                .tradedPrice(entity.getTradedPrice())
                .isin(entity.getIsin())
                .issuingCompanyAfcName(entity.getIssuingCompanyAfcName())
                .instrumentAfcNormName(entity.getInstrumentAfcNormName())
                .build();
    }

    public static List<TurnoverResponseDTO> adaptList(List<TurnoverEntity> entities) {
        return entities.stream()
                .filter(e -> e.getType() != TurnoverOperationType.REMAINING)
                .map(TurnoverTransformer::adapt)
                .toList();
    }
}
