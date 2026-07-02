package ir.ebb.external.rayan.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.external.rayan.wallet.dto.RayanWalletDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Table(name = "rayan_wallet")
public class RayanWalletEntity extends BaseEntityById {

    private Long accountNumber;
    private String nationalCode;
    private Long customerCredit;
    private Long financialRemain;
    private Long inProgress;
    private Long bond;
    private Long loan;
    private Long saleT0;
    private Long saleT1;
    private Long saleT2;
    private Long purchaseT0;
    private Long purchaseT1;
    private Long purchaseT2;

    public RayanWalletEntity(Long accountNumber) {
        this.accountNumber = accountNumber;
        this.customerCredit = 0L;
        this.financialRemain = 0L;
        this.inProgress = 0L;
        this.bond = 0L;
        this.loan = 0L;
        this.saleT0 = 0L;
        this.saleT1 = 0L;
        this.saleT2 = 0L;
        this.purchaseT0 = 0L;
        this.purchaseT1 = 0L;
        this.purchaseT2 = 0L;
    }

    public RayanWalletDTO toDTO() {
        return RayanWalletDTO.builder()
                .accountNumber(this.accountNumber)
                .nationalCode(this.nationalCode)
                .customerCredit(this.customerCredit)
                .financialRemain(this.financialRemain)
                .inProgress(this.inProgress)
                .bond(this.bond)
                .loan(this.loan)
                .saleT0(this.saleT0)
                .saleT1(this.saleT1)
                .saleT2(this.saleT2)
                .purchaseT0(this.purchaseT0)
                .purchaseT1(this.purchaseT1)
                .purchaseT2(this.purchaseT2)
                .build();
    }
}
