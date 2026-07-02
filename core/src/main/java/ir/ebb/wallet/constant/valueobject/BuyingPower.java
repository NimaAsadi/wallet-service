package ir.ebb.wallet.constant.valueobject;

public record BuyingPower(Long balance, Long credit, Long separCredit) {

    public Long sum(boolean includeSeparCredit) {
        return includeSeparCredit ? balance + credit + separCredit : balance + credit;
    }
}
