package ir.ebb.wallet.app.admin.dto.request;

import ir.ebb.common.dto.request.PaginatedRequestDTO;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.UUID;

@Getter
@Setter
public class WalletTransactionSearchRequestDTO extends PaginatedRequestDTO {

    private String userId;
    private Long accountNumber;
    private WalletTransactionType type;
    private UUID trackingCode;

    public WalletTransactionSearchRequestDTO(Integer page, Integer size,
                                              String userId, Long accountNumber,
                                              WalletTransactionType type, UUID trackingCode,
                                              String orderByProperty, Sort.Direction orderByDirection) {
        super(page, size, orderByProperty, orderByDirection);
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.trackingCode = trackingCode;
    }
}
