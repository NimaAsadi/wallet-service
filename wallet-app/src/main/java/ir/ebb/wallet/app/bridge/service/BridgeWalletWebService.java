package ir.ebb.wallet.app.bridge.service;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;

public interface BridgeWalletWebService {

    BridgeWalletResponseDTO getWalletDetails(Long accountNumber);
}
