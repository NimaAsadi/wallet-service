package ir.ebb.wallet.projection.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import ir.ebb.common.repository.Column;
import ir.ebb.common.repository.GenerateRepository;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@GenerateRepository(table = "credit_history")
public class CreditHistoryEntity {

    private UUID id;
    private Long accountNumber;
    private Long amount;
    private RayanCreditStatus status = RayanCreditStatus.PENDING;
    private UUID createdId;
    private String createdBy;
    private String errorMessage;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(insertable = false)
    private LocalDateTime updatedAt;

    /**
     * Admin credit-op audit row. The {@code id} is minted here (the generated INSERT binds it —
     * no schema default) and doubles as the actor-side {@code AddCredit} idempotency key.
     */
    public CreditHistoryEntity(Long accountNumber, Long amount, RayanCreditStatus status,
                               UUID createdId, String createdBy) {
        this.id = UuidCreator.getTimeOrderedEpoch();
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.status = status;
        this.createdId = createdId;
        this.createdBy = createdBy;
    }
}
