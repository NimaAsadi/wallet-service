package ir.ebb.wallet.app.admin.transformer;

import ir.ebb.wallet.app.admin.dto.request.CreditHistorySearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.CreditHistoryResponseDTO;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.projection.entity.CreditHistoryEntity;

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
                req.getCreatedBy());
    }

    public static CreditHistoryResponseDTO adapt(CreditHistoryEntity e) {
        Long createdAt = e.getCreatedAt() != null
                ? e.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                : null;
        // The user-derived fields (nationalCode/fullName/accountName/fatherName) have no source in
        // the projection credit_history table — it carries accountNumber/createdBy only.
        return new CreditHistoryResponseDTO(
                e.getId(),
                createdAt,
                e.getCreatedBy(),
                null,
                null,
                null,
                null,
                e.getAmount(),
                e.getStatus(),
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getErrorMessage()
        );
    }
}
