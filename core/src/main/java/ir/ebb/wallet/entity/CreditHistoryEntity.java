package ir.ebb.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreditHistoryEntity extends BaseEntityById {

    private Long accountNumber;
    private Long amount;
    private RayanCreditStatus status = RayanCreditStatus.PENDING;
    private UUID createdId;
    private String createdBy;
    private String errorMessage;

    public CreditHistoryEntity(Long accountNumber, Long amount, RayanCreditStatus status, UUID createdId, String createdBy) {
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.status = status;
        this.createdId = createdId;
        this.createdBy = createdBy;
    }
}
