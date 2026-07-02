package ir.ebb.wallet.app.admin.transformer;

import ir.ebb.wallet.app.admin.dto.request.CreditHistorySearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.CreditHistoryResponseDTO;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.entity.CreditHistoryEntity;

import java.time.ZoneOffset;

public final class CreditHistoryTransformer {

    private CreditHistoryTransformer() {}

    public static CreditSpecificationDTO adapt(CreditHistorySearchRequestDTO req) {
        return new CreditSpecificationDTO(
                req.getUserIds(),
                req.getFromDate(),
                req.getToDate(),
                req.getFromAmount(),
                req.getToAmount(),
                req.getStatus(),
                req.getCreatedBy(),
                req.getOrderBy());
    }

    public static CreditHistoryResponseDTO adapt(CreditHistoryEntity e) {
        Long createdAt = e.getCreatedAt() != null
                ? e.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                : null;
        return new CreditHistoryResponseDTO(
                e.getId(),
                createdAt,
                e.getCreatedBy(),
                e.getUser().getNationalCode(),
                e.getUser().getFullName(),
                e.getUser().getAccountName(),
                e.getUser().getFatherName(),
                e.getAmount(),
                e.getStatus(),
                e.getStatus().name(),
                e.getErrorMessage()
        );
    }
}
