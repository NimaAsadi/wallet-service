package ir.ebb.external.rayan.wallet.entity;

import ir.ebb.common.model.base.BaseEntityById;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Table(name = "rayan_wallet_history")
public class RayanWalletHistoryEntity extends BaseEntityById {

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
}
