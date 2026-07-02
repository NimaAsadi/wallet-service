package ir.ebb.wallet.app.bridge.dto.request;

import ir.ebb.common.dto.request.PaginatedRequestDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

@Getter
@Setter
public class BridgeTurnoverSearchRequestDTO extends PaginatedRequestDTO {

    private LocalDate fromCreatedAt;
    private LocalDate toCreatedAt;

    public BridgeTurnoverSearchRequestDTO(Integer page, Integer size,
                                           LocalDate fromCreatedAt, LocalDate toCreatedAt,
                                           String orderByProperty, Sort.Direction orderByDirection) {
        super(page, size, orderByProperty, orderByDirection);
        this.fromCreatedAt = fromCreatedAt;
        this.toCreatedAt = toCreatedAt;
    }
}
