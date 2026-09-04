package ir.ebb.wallet.app.user.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.wallet.app.user.dto.response.WalletResponseDTO;
import ir.ebb.wallet.app.user.transformer.WalletTransformer;
import ir.ebb.wallet.wallet.WalletFacade;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WalletWebServiceImpl implements WalletWebService {

    private final WalletFacade walletFacade;

    @Override
    public WalletResponseDTO getWalletDetails(UserPrincipal principal) {
        return WalletTransformer.adapt(walletFacade.getWallet(principal.asUser().getDbsAccountNumber()));
    }
}
