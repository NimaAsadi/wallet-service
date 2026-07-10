package ir.ebb.wallet.entity;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.model.base.BaseEntityById;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = true)
public class WalletEntity extends BaseEntityById {

    private User user;

    private WalletParameterEmbedded t0 = new WalletParameterEmbedded();
    private WalletParameterEmbedded t1 = new WalletParameterEmbedded();
    private WalletParameterEmbedded t2 = new WalletParameterEmbedded();

    private Long credit = 0L;
    private Long initialCredit = 0L;
    private Long separCredit = 0L;
    private Long separInitialCredit = 0L;

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
