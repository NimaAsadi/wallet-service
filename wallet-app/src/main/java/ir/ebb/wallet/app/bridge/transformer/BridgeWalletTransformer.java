package ir.ebb.wallet.app.bridge.transformer;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;
import ir.ebb.wallet.aggregate.Wallet;
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

    /** Same projection as {@link #adapt(WalletEntity)}, sourced from the authoritative aggregate state. */
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
