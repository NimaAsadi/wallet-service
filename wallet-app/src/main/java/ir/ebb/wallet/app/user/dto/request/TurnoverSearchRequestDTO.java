package ir.ebb.wallet.app.user.dto.request;

import ir.ebb.common.dto.request.PaginatedRequestDTO;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class TurnoverSearchRequestDTO extends PaginatedRequestDTO {

    private LocalDate fromCreatedAt;
    private LocalDate toCreatedAt;

    private List<TurnoverOperationType> types;
    private boolean withDetail;
    private boolean withPreBalance;

    public TurnoverSearchRequestDTO() {
        this.fromCreatedAt = LocalDate.now();
        this.toCreatedAt = LocalDate.now();
    }

    public TurnoverSearchRequestDTO(int page, int size) {
        setPage(page);
        setSize(size);
    }
}
