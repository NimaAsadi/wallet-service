package ir.ebb.wallet.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TurnoverSpecificationDTO(
        Long dbsAccountNumber,
        LocalDateTime fromCreatedAt,
        LocalDateTime toCreatedAt,
        Boolean withPreBalance
) {}
