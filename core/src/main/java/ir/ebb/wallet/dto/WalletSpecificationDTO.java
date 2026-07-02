package ir.ebb.wallet.dto;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record WalletSpecificationDTO(
        UUID userId,
        Long dbsAccountNumber,
        Set<UUID> userIds,
        Set<Long> accountNumbers,
        Long fromT0Balance, Long toT0Balance,
        Long fromT1Balance, Long toT1Balance,
        Long fromT2Balance, Long toT2Balance,
        Long fromInitialCredit, Long toInitialCredit,
        Long fromCredit, Long toCredit,
        Long fromFrozen, Long toFrozen,
        Boolean separCreditDebtor
) {}
