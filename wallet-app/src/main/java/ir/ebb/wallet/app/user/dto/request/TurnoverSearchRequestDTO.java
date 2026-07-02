package ir.ebb.wallet.app.user.dto.request;

import ir.ebb.common.dto.request.PaginatedRequestDTO;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class TurnoverSearchRequestDTO extends PaginatedRequestDTO {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromCreatedAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toCreatedAt;

    private List<TurnoverOperationType> types;
    private boolean withDetail;
    private boolean withPreBalance;

    public TurnoverSearchRequestDTO() {
        super(0, 50, "createdAt", Sort.Direction.DESC);
        this.fromCreatedAt = LocalDate.now();
        this.toCreatedAt = LocalDate.now();
    }

    public TurnoverSearchRequestDTO(int page, int size) {
        super(page, size, "createdAt", Sort.Direction.DESC);
    }
}
