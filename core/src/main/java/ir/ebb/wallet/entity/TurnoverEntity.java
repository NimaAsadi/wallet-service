package ir.ebb.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
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
@Table(name = "turnover")
@EqualsAndHashCode(callSuper = true)
public class TurnoverEntity extends BaseEntityById {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "keycloakId", column = @Column(name = "user_id", nullable = false)),
            @AttributeOverride(name = "dbsAccountNumber", column = @Column(name = "account_number", nullable = false))
    })
    private User user;

    @Column(nullable = false)
    private UUID walletId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TurnoverOperationType type;

    @ColumnDefault("0") @Column(nullable = false) private Long debit  = 0L;
    @ColumnDefault("0") @Column(nullable = false) private Long credit = 0L;

    @Column(nullable = false)
    private UUID trackingId;

    @ColumnDefault("0") @Column(nullable = false) private Long tradedQuantity = 0L;
    @ColumnDefault("0") @Column(nullable = false) private Long tradedPrice    = 0L;

    private Integer tradeNumber;
    private String isin;
    private String issuingCompanyAfcName;
    private String instrumentAfcNormName;
    private String receiptBankNumber;
    private Long withdrawRayanId;

    public TurnoverEntity(User user, UUID walletId, TurnoverOperationType type, Long credit, UUID trackingId) {
        this.user = user; this.walletId = walletId; this.type = type;
        this.credit = credit; this.trackingId = trackingId;
    }

    public TurnoverEntity(User user, UUID walletId, TurnoverOperationType type,
                          Long debit, Long credit, UUID trackingId,
                          Long tradeQuantity, Long price, Integer tradeNumber,
                          String isin, String companyName, String instrumentName) {
        this.user = user; this.walletId = walletId; this.type = type;
        this.debit = debit; this.credit = credit; this.trackingId = trackingId;
        this.tradedQuantity = tradeQuantity; this.tradedPrice = price;
        this.tradeNumber = tradeNumber; this.isin = isin;
        this.issuingCompanyAfcName = companyName; this.instrumentAfcNormName = instrumentName;
    }

    public TurnoverEntity(User user, UUID walletId, TurnoverOperationType type,
                          Long debit, UUID trackingId, Long resultRayanId) {
        this.user = user; this.walletId = walletId; this.type = type;
        this.debit = debit; this.trackingId = trackingId; this.withdrawRayanId = resultRayanId;
    }

    public TurnoverEntity(User user, UUID walletId, TurnoverOperationType type,
                          Long credit, UUID trackingId, String receiptBankNumber) {
        this.user = user; this.walletId = walletId; this.type = type;
        this.credit = credit; this.trackingId = trackingId; this.receiptBankNumber = receiptBankNumber;
    }
}
