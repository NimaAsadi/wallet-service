package ir.ebb.wallet.dto;

import ir.ebb.common.model.user.User;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TurnoverSpecificationDTO(
        User user,
        LocalDateTime fromCreatedAt,
        LocalDateTime toCreatedAt,
        Boolean withPreBalance
) {}
