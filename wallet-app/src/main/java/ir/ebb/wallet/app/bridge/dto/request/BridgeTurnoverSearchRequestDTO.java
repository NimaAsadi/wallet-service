package ir.ebb.wallet.app.bridge.dto.request;

import ir.ebb.common.dto.request.Direction;
import ir.ebb.common.dto.request.PaginatedRequestDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class BridgeTurnoverSearchRequestDTO extends PaginatedRequestDTO {

    private LocalDate fromCreatedAt;
    private LocalDate toCreatedAt;

    public BridgeTurnoverSearchRequestDTO(Integer page, Integer size,
                                           LocalDate fromCreatedAt, LocalDate toCreatedAt,
                                           String orderByProperty, Direction orderByDirection) {
        setPage(page);
        setSize(size);
        setOrderByProperty(orderByProperty);
        setOrderByDirection(orderByDirection);
        this.fromCreatedAt = fromCreatedAt;
        this.toCreatedAt = toCreatedAt;
    }
}
