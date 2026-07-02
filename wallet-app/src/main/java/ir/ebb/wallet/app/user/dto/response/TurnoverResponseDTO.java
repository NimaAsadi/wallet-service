package ir.ebb.wallet.app.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class TurnoverResponseDTO {
    private String trackingId;
    private Long createdAt;
    private Long debit;
    private Long credit;
    private String comment;
    private Long balance;
    @JsonIgnore private Long tradedPrice;
    @JsonIgnore private TurnoverOperationType type;
    @JsonIgnore private Long tradedQuantity;
    @JsonIgnore private String isin;
    @JsonIgnore private String issuingCompanyAfcName;
    @JsonIgnore private String instrumentAfcNormName;

    @JsonIgnore
    public Long getTradedValue() {
        if (tradedPrice == null || tradedQuantity == null || tradedPrice == 0 || tradedQuantity == 0)
            return 0L;
        return tradedQuantity * tradedPrice;
    }
}
