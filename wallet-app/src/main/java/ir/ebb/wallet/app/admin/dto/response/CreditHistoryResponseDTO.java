package ir.ebb.wallet.app.admin.dto.response;

import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;

import java.util.UUID;

public record CreditHistoryResponseDTO(
        UUID trackingCode,
        Long createdAt,
        String createdBy,
        String nationalCode,
        String fullName,
        String accountName,
        String fatherName,
        Long amount,
        RayanCreditStatus status,
        String statusLocale,
        String errorMessage
) {}
