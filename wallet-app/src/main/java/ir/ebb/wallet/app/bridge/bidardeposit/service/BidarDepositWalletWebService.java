package ir.ebb.wallet.app.bridge.bidardeposit.service;

import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletDepositRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletFreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletSpendRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletUnfreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.response.BidarDepositWalletResponseDTO;

public interface BidarDepositWalletWebService {

    BidarDepositWalletResponseDTO deposit(BidarDepositWalletDepositRequestDTO request);

    BidarDepositWalletResponseDTO freeze(BidarDepositWalletFreezeRequestDTO request);

    BidarDepositWalletResponseDTO unfreeze(BidarDepositWalletUnfreezeRequestDTO request);

    BidarDepositWalletResponseDTO spend(BidarDepositWalletSpendRequestDTO request);
}
