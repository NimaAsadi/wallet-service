package ir.ebb.wallet.app.user.transformer;

import ir.ebb.wallet.app.user.dto.response.WalletResponseDTO;
import ir.ebb.wallet.valueobject.Wallet;

public final class WalletTransformer {

    private WalletTransformer() {}

    public static WalletResponseDTO adapt(Wallet wallet) {
        Long balance = wallet.getT2().getBalance() + wallet.getT1().getBalance() + wallet.getT0().getBalance();
        Long frozen  = wallet.getT2().getFrozen()  + wallet.getT1().getFrozen()  + wallet.getT0().getFrozen();
        return WalletResponseDTO.builder()
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
