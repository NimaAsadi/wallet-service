package ir.ebb.wallet.app.user.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.wallet.app.user.dto.response.WalletResponseDTO;
import ir.ebb.wallet.app.user.transformer.WalletTransformer;
import ir.ebb.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WalletWebServiceImpl implements WalletWebService {

    private final WalletService walletService;

    @Override
    public CompletionStage<WalletResponseDTO> getWalletDetails(UserPrincipal principal) {
        // Entity-served (strongly consistent) — never behind the projection.
        return walletService.getWallet(principal.asUser())
                .thenApply(snapshot -> WalletTransformer.adapt(snapshot.toWallet()));
    }
}
