package ir.ebb.wallet.projection.entity;

import ir.ebb.common.repository.Column;
import ir.ebb.common.repository.GenerateRepository;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@GenerateRepository(table = "turnover")
public class TurnoverEntity {

    private UUID id;
    private Long accountNumber;
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


    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(insertable = false)
    private LocalDateTime updatedAt;

    public TurnoverEntity(long accountNumber, UUID walletId, TurnoverOperationType type, Long credit, UUID trackingId) {
        this.accountNumber = accountNumber;
        this.walletId = walletId;
        this.type = type;
        this.credit = credit;
        this.trackingId = trackingId;
    }

    public TurnoverEntity(
            long accountNumber,
            UUID walletId,
            TurnoverOperationType type,
            Long debit,
            Long credit,
            UUID trackingId,
            Long tradedQuantity,
            Long price,
            Integer tradeNumber,
            String isin,
            String companyName,
            String instrumentName
    ) {
        this.accountNumber = accountNumber;
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

    public TurnoverEntity(
            long accountNumber,
            UUID walletId,
            TurnoverOperationType type,
            Long debit,
            UUID trackingId,
            Long resultRayanId
    ) {
        this.accountNumber = accountNumber;
        this.walletId = walletId;
        this.type = type;
        this.debit = debit;
        this.trackingId = trackingId;
        this.withdrawRayanId = resultRayanId;
    }

    public TurnoverEntity(
            long accountNumber,
            UUID walletId,
            TurnoverOperationType type,
            Long credit,
            UUID trackingId,
            String receiptBankNumber
    ) {
        this.accountNumber = accountNumber;
        this.walletId = walletId;
        this.type = type;
        this.credit = credit;
        this.trackingId = trackingId;
        this.receiptBankNumber = receiptBankNumber;
    }
}
