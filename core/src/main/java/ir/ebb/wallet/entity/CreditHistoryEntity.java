package ir.ebb.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.userinfo.entity.UserEntity;
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

    private UserEntity user;
    private Long amount;
    private RayanCreditStatus status = RayanCreditStatus.PENDING;
    private UUID createdId;
    private String createdBy;
    private String errorMessage;

    public CreditHistoryEntity(UserEntity user, Long amount, RayanCreditStatus status, UUID createdId, String createdBy) {
        this.user = user;
        this.amount = amount;
        this.status = status;
        this.createdId = createdId;
        this.createdBy = createdBy;
    }
}
