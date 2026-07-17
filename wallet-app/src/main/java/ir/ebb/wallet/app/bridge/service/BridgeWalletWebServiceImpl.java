package ir.ebb.wallet.app.bridge.service;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;
import ir.ebb.wallet.app.bridge.transformer.BridgeWalletTransformer;
import ir.ebb.wallet.wallet.WalletFacade;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BridgeWalletWebServiceImpl implements BridgeWalletWebService {

    private final WalletFacade walletFacade;

    @Override
    public BridgeWalletResponseDTO getWalletDetails(Long accountNumber) {
        return BridgeWalletTransformer.adapt(walletFacade.getWallet(accountNumber));
    }
}
