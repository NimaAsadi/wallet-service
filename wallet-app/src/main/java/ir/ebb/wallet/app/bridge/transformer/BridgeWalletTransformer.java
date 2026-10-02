package ir.ebb.wallet.app.bridge.transformer;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;
import ir.ebb.wallet.valueobject.Wallet;

public final class BridgeWalletTransformer {

    private BridgeWalletTransformer() {}

    public static BridgeWalletResponseDTO adapt(Wallet wallet) {
        long balance = wallet.getT0().getBalance() + wallet.getT1().getBalance() + wallet.getT2().getBalance();
        long frozen = wallet.getT0().getFrozen() + wallet.getT1().getFrozen() + wallet.getT2().getFrozen();
        return BridgeWalletResponseDTO.builder()
                .balance(balance)
                .t0(wallet.getT0().getBalance())
                .t1(wallet.getT1().getBalance() + wallet.getT0().getBalance())
                .t2(wallet.getT2().getBalance() + wallet.getT1().getBalance() + wallet.getT0().getBalance())
                .totalFrozen(frozen)
                .credit(wallet.getCredit())
                .initCredit(wallet.getInitialCredit())
                .separCredit(wallet.getSeparCredit())
                .separInitCredit(wallet.getSeparInitialCredit())
                .build();
    }
}
