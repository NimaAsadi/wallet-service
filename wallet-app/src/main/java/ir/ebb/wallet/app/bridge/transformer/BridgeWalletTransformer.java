package ir.ebb.wallet.app.bridge.transformer;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;
import ir.ebb.wallet.entity.WalletEntity;

public final class BridgeWalletTransformer {

    private BridgeWalletTransformer() {}

    public static BridgeWalletResponseDTO adapt(WalletEntity e) {
        long balance = e.getT0().getBalance() + e.getT1().getBalance() + e.getT2().getBalance();
        long frozen = e.getT0().getFrozen() + e.getT1().getFrozen() + e.getT2().getFrozen();
        return BridgeWalletResponseDTO.builder()
                .balance(balance)
                .t0(e.getT0().getBalance())
                .t1(e.getT1().getBalance() + e.getT0().getBalance())
                .t2(e.getT2().getBalance() + e.getT1().getBalance() + e.getT0().getBalance())
                .totalFrozen(frozen)
                .credit(e.getCredit())
                .initCredit(e.getInitialCredit())
                .separCredit(e.getSeparCredit())
                .separInitCredit(e.getSeparInitialCredit())
                .build();
    }
}
