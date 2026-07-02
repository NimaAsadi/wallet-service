package ir.ebb.wallet.entity;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Table(name = "wallet", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id"),
        @UniqueConstraint(columnNames = "account_number")
})
@EqualsAndHashCode(callSuper = true)
public class WalletEntity extends BaseEntityById {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "keycloakId", column = @Column(name = "user_id", nullable = false)),
            @AttributeOverride(name = "dbsAccountNumber", column = @Column(name = "account_number", nullable = false))
    })
    private User user;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "balance", column = @Column(name = "t0_balance")),
            @AttributeOverride(name = "frozen", column = @Column(name = "t0_frozen"))
    })
    private WalletParameterEmbedded t0 = new WalletParameterEmbedded();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "balance", column = @Column(name = "t1_balance")),
            @AttributeOverride(name = "frozen", column = @Column(name = "t1_frozen"))
    })
    private WalletParameterEmbedded t1 = new WalletParameterEmbedded();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "balance", column = @Column(name = "t2_balance")),
            @AttributeOverride(name = "frozen", column = @Column(name = "t2_frozen"))
    })
    private WalletParameterEmbedded t2 = new WalletParameterEmbedded();

    @ColumnDefault("0")
    @Column(nullable = false)
    private Long credit = 0L;

    @ColumnDefault("0")
    @Column(nullable = false)
    private Long initialCredit = 0L;

    @ColumnDefault("0")
    @Column(nullable = false)
    private Long separCredit = 0L;

    @ColumnDefault("0")
    @Column(nullable = false)
    private Long separInitialCredit = 0L;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "wallet", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private WalletDebtEntity walletDebtEntity;

    public BuyingPower buyingPower(SettlementDelay settlementDelay) {
        Long totalBalance = switch (settlementDelay) {
            case T_PLUS_2 -> t2.getBalance() + t1.getBalance() + t0.getBalance();
            case T_PLUS_1 -> t1.getBalance() + t0.getBalance();
            case T_PLUS_0 -> t0.getBalance();
        };
        return new BuyingPower(totalBalance, credit, separCredit);
    }

    public Wallet adaptToDomain() {
        Wallet wallet = new Wallet(this.getUser());
        wallet.setVersion(this.getVersion());
        wallet.setId(super.getId());
        wallet.setCredit(this.getCredit());
        wallet.setInitialCredit(this.getInitialCredit());
        wallet.setSeparCredit(this.getSeparCredit());
        wallet.setSeparInitialCredit(this.getSeparInitialCredit());
        wallet.setT0(t0.adoptToDomain());
        wallet.setT1(t1.adoptToDomain());
        wallet.setT2(t2.adoptToDomain());
        if (this.getWalletDebtEntity() != null)
            wallet.setWalletDebt(this.getWalletDebtEntity().adaptToDomain());
        return wallet;
    }

    public Long getTotalAsset() {
        return this.t0.getBalance()
                + this.t0.getFrozen()
                + this.t1.getBalance()
                + this.t1.getFrozen()
                + this.t2.getBalance()
                + this.t2.getFrozen();
    }
}
