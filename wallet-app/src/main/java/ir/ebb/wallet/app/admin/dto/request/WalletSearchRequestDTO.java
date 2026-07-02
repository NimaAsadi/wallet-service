package ir.ebb.wallet.app.admin.dto.request;

import ir.ebb.common.dto.request.PaginatedRequestDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class WalletSearchRequestDTO extends PaginatedRequestDTO {

    private Set<UUID> userIds;
    private Set<Long> accountNumbers;
    private Long fromT0Balance;
    private Long toT0Balance;
    private Long fromT1Balance;
    private Long toT1Balance;
    private Long fromT2Balance;
    private Long toT2Balance;
    private Long fromInitialCredit;
    private Long toInitialCredit;
    private Long fromCredit;
    private Long toCredit;
    private Long fromFrozen;
    private Long toFrozen;

    public WalletSearchRequestDTO(Integer page, Integer size, String orderByProperty, Sort.Direction orderByDirection,
                                   Set<UUID> userIds, Set<Long> accountNumbers,
                                   Long fromT0Balance, Long toT0Balance,
                                   Long fromT1Balance, Long toT1Balance,
                                   Long fromT2Balance, Long toT2Balance,
                                   Long fromInitialCredit, Long toInitialCredit,
                                   Long fromCredit, Long toCredit,
                                   Long fromFrozen, Long toFrozen) {
        super(page, size, orderByProperty, orderByDirection);
        this.userIds = userIds;
        this.accountNumbers = accountNumbers;
        this.fromT0Balance = fromT0Balance;
        this.toT0Balance = toT0Balance;
        this.fromT1Balance = fromT1Balance;
        this.toT1Balance = toT1Balance;
        this.fromT2Balance = fromT2Balance;
        this.toT2Balance = toT2Balance;
        this.fromInitialCredit = fromInitialCredit;
        this.toInitialCredit = toInitialCredit;
        this.fromCredit = fromCredit;
        this.toCredit = toCredit;
        this.fromFrozen = fromFrozen;
        this.toFrozen = toFrozen;
    }
}
