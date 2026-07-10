package ir.ebb.wallet.app.admin.dto.request;

import ir.ebb.common.dto.request.Direction;
import ir.ebb.common.dto.request.PaginatedRequestDTO;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreditHistorySearchRequestDTO extends PaginatedRequestDTO {

    private Set<UUID> userIds;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Long fromAmount;
    private Long toAmount;
    private RayanCreditStatus status;
    private String createdBy;

    public CreditHistorySearchRequestDTO(Integer page, Integer size,
                                          Set<UUID> userIds,
                                          Long fromDate, Long toDate,
                                          Long fromAmount, Long toAmount,
                                          RayanCreditStatus status, String createdBy,
                                          String orderByProperty, Direction orderByDirection) {
        setPage(page);
        setSize(size);
        setOrderByProperty(orderByProperty);
        setOrderByDirection(orderByDirection);
        this.userIds = userIds;
        this.fromDate = toLocalDate(fromDate);
        this.toDate = toLocalDate(toDate);
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.status = status;
        this.createdBy = createdBy;
    }

    private static LocalDate toLocalDate(Long epochMilli) {
        if (epochMilli == null) return null;
        return Instant.ofEpochMilli(epochMilli).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
