package ir.ebb.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TurnoverEntity extends BaseEntityById {

    private User user;
    private UUID walletId;
    private TurnoverOperationType type;
    private Long debit = 0L;
    private Long credit = 0L;
    private UUID trackingId;
    private Long tradedQuantity = 0L;
    private Long tradedPrice = 0L;
    private Integer tradeNumber;
    private String isin;
    private String issuingCompanyAfcName;
    private String instrumentAfcNormName;
    private String receiptBankNumber;
    private Long withdrawRayanId;

    public TurnoverEntity(User user, UUID walletId, TurnoverOperationType type, Long credit, UUID trackingId) {
        this.user = user;
        this.walletId = walletId;
        this.type = type;
        this.credit = credit;
        this.trackingId = trackingId;
    }

    public TurnoverEntity(User user, UUID walletId, TurnoverOperationType type,
                          Long debit, Long credit, UUID trackingId,
                          Long tradedQuantity, Long price, Integer tradeNumber,
                          String isin, String companyName, String instrumentName) {
        this.user = user;
        this.walletId = walletId;
        this.type = type;
        this.debit = debit;
        this.credit = credit;
        this.trackingId = trackingId;
        this.tradedQuantity = tradedQuantity;
        this.tradedPrice = price;
        this.tradeNumber = tradeNumber;
        this.isin = isin;
        this.issuingCompanyAfcName = companyName;
        this.instrumentAfcNormName = instrumentName;
    }

    public TurnoverEntity(User user, UUID walletId, TurnoverOperationType type,
                          Long debit, UUID trackingId, Long resultRayanId) {
        this.user = user;
        this.walletId = walletId;
        this.type = type;
        this.debit = debit;
        this.trackingId = trackingId;
        this.withdrawRayanId = resultRayanId;
    }

    public TurnoverEntity(User user, UUID walletId, TurnoverOperationType type,
                          Long credit, UUID trackingId, String receiptBankNumber) {
        this.user = user;
        this.walletId = walletId;
        this.type = type;
        this.credit = credit;
        this.trackingId = trackingId;
        this.receiptBankNumber = receiptBankNumber;
    }
}
