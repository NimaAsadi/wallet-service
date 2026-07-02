package ir.ebb.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.userinfo.entity.UserEntity;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "credit_history")
@EqualsAndHashCode(callSuper = true)
public class CreditHistoryEntity extends BaseEntityById {

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false),
            @JoinColumn(name = "account_number", referencedColumnName = "account_number", nullable = false)
    })
    private UserEntity user;

    @ColumnDefault("0")
    @Column(nullable = false)
    private Long amount;

    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
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
