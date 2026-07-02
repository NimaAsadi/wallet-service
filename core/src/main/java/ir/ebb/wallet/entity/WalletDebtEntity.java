package ir.ebb.wallet.entity;

import ir.ebb.common.model.base.BaseEntity;
import ir.ebb.wallet.aggregate.WalletDebt;
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
@Table(name = "wallet_debt")
@EqualsAndHashCode(callSuper = true)
public class WalletDebtEntity extends BaseEntity {

    @Id
    @Column(name = "wallet_id")
    private UUID id;

    @ColumnDefault("0") @Column(nullable = false, name = "t2_to_t0_debt")     private Long t2Tot0Debt = 0L;
    @ColumnDefault("0") @Column(nullable = false, name = "t2_to_t1_debt")     private Long t2Tot1Debt = 0L;
    @ColumnDefault("0") @Column(nullable = false, name = "t1_to_t0_debt")     private Long t1Tot0Debt = 0L;
    @ColumnDefault("0") @Column(nullable = false, name = "t2_to_credit_debt") private Long t2ToCreditDebt = 0L;
    @ColumnDefault("0") @Column(nullable = false, name = "t1_to_credit_debt") private Long t1ToCreditDebt = 0L;
    @ColumnDefault("0") @Column(nullable = false, name = "t2_to_separ_credit_debt") private Long t2ToSeparCreditDebt = 0L;
    @ColumnDefault("0") @Column(nullable = false, name = "t1_to_separ_credit_debt") private Long t1ToSeparCreditDebt = 0L;

    @OneToOne
    @MapsId
    @JoinColumn(name = "wallet_id")
    private WalletEntity wallet;

    public WalletDebt adaptToDomain() {
        WalletDebt walletDebt = new WalletDebt();
        walletDebt.setT2Tot0Debt(this.getT2Tot0Debt());
        walletDebt.setT2Tot1Debt(this.getT2Tot1Debt());
        walletDebt.setT1Tot0Debt(this.getT1Tot0Debt());
        walletDebt.setT2ToCreditDebt(this.getT2ToCreditDebt());
        walletDebt.setT1ToCreditDebt(this.getT1ToCreditDebt());
        walletDebt.setT2ToSeparCreditDebt(this.getT2ToSeparCreditDebt());
        walletDebt.setT1ToSeparCreditDebt(this.getT1ToSeparCreditDebt());
        return walletDebt;
    }

    public void clear() {
        this.t2Tot0Debt = 0L; this.t2Tot1Debt = 0L; this.t1Tot0Debt = 0L;
        this.t2ToCreditDebt = 0L; this.t1ToCreditDebt = 0L;
        this.t2ToSeparCreditDebt = 0L; this.t1ToSeparCreditDebt = 0L;
    }

    public WalletDebtEntity(Long t2Tot0Debt, Long t2Tot1Debt, Long t1Tot0Debt,
                             Long t2ToCreditDebt, Long t1ToCreditDebt,
                             Long t2ToSeparCreditDebt, Long t1ToSeparCreditDebt) {
        this.t2Tot0Debt = t2Tot0Debt; this.t2Tot1Debt = t2Tot1Debt; this.t1Tot0Debt = t1Tot0Debt;
        this.t2ToCreditDebt = t2ToCreditDebt; this.t1ToCreditDebt = t1ToCreditDebt;
        this.t2ToSeparCreditDebt = t2ToSeparCreditDebt; this.t1ToSeparCreditDebt = t1ToSeparCreditDebt;
    }

    public WalletDebtEntity(WalletEntity wallet) {
        this.clear();
        this.wallet = wallet;
    }
}
