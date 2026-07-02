package ir.ebb.wallet.app.admin.dto.response;

import lombok.Data;

@Data
public class RayanWalletResponseDTO {
    private Long accountNumber;
    private Long t0Frozen;
    private Long t0BuyingPower;
    private Long t1Frozen;
    private Long t1BuyingPower;
    private Long t2Frozen;
    private Long t2BuyingPower;
    private Long credit;

    public Long getT0Balance() { return t0BuyingPower + t0Frozen; }
    public Long getT1Balance() { return t1BuyingPower + t1Frozen; }
    public Long getT2Balance() { return t2BuyingPower + t2Frozen; }
    public Long getT0BuyingPowerAggregate() { return t0BuyingPower; }
    public Long getT1BuyingPowerAggregate() { return t1BuyingPower + getT0BuyingPowerAggregate(); }
    public Long getT2BuyingPowerAggregate() { return t2BuyingPower + getT1BuyingPowerAggregate(); }
    public Long getFrozen() { return t0Frozen + t1Frozen + t2Frozen; }
}
