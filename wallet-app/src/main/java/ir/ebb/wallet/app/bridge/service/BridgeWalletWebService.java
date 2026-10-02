package ir.ebb.wallet.app.bridge.service;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;

import java.util.concurrent.CompletionStage;

public interface BridgeWalletWebService {

    CompletionStage<BridgeWalletResponseDTO> getWalletDetails(Long accountNumber);
}
