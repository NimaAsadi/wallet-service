package ir.ebb.wallet.app.bridge.service;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;
import ir.ebb.wallet.app.bridge.transformer.BridgeWalletTransformer;
import ir.ebb.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BridgeWalletWebServiceImpl implements BridgeWalletWebService {

    private final WalletService walletService;

    @Override
    public CompletionStage<BridgeWalletResponseDTO> getWalletDetails(Long accountNumber) {
        // Entity-served (strongly consistent) — never behind the projection.
        return walletService.getWallet(accountNumber)
                .thenApply(snapshot -> BridgeWalletTransformer.adapt(snapshot.toWallet()));
    }
}
