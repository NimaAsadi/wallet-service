package ir.ebb.wallet.app.bridge.bidardeposit.service;

import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletDepositRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletFreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletSpendRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletUnfreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.response.BidarDepositWalletResponseDTO;

import java.util.concurrent.CompletionStage;

public interface BidarDepositWalletWebService {

    CompletionStage<BidarDepositWalletResponseDTO> deposit(BidarDepositWalletDepositRequestDTO request);

    CompletionStage<BidarDepositWalletResponseDTO> freeze(BidarDepositWalletFreezeRequestDTO request);

    CompletionStage<BidarDepositWalletResponseDTO> unfreeze(BidarDepositWalletUnfreezeRequestDTO request);

    CompletionStage<BidarDepositWalletResponseDTO> spend(BidarDepositWalletSpendRequestDTO request);
}
