package ir.ebb.wallet.app.bridge.service;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;
import ir.ebb.wallet.app.bridge.transformer.BridgeWalletTransformer;
import ir.ebb.wallet.service.query.WalletQueryService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BridgeWalletWebServiceImpl implements BridgeWalletWebService {

    private final WalletQueryService walletQueryService;

    @Override
    public BridgeWalletResponseDTO getWalletDetails(Long accountNumber) {
        return BridgeWalletTransformer.adapt(walletQueryService.getWalletEntity(accountNumber));
    }
}
