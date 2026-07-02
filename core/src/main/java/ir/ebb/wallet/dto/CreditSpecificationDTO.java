package ir.ebb.wallet.dto;

import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CreditSpecificationDTO(
        Set<UUID> userIds,
        LocalDate fromDate,
        LocalDate toDate,
        Long fromAmount,
        Long toAmount,
        RayanCreditStatus status,
        String createdBy,
        Sort.Order order
) {}
