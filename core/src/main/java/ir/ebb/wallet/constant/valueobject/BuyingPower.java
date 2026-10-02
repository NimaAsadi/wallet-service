package ir.ebb.wallet.constant.valueobject;

import ir.ebb.wallet.serialization.WalletSerializable;

public record BuyingPower(Long balance, Long credit, Long separCredit) implements WalletSerializable {

    public Long sum(boolean includeSeparCredit) {
        return includeSeparCredit ? balance + credit + separCredit : balance + credit;
    }
}
