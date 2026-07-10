package ir.ebb.wallet.app.user.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.wallet.app.user.dto.response.WalletResponseDTO;
import ir.ebb.wallet.app.user.transformer.WalletTransformer;
import ir.ebb.wallet.service.query.WalletQueryService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WalletWebServiceImpl implements WalletWebService {

    private final WalletQueryService walletQueryService;

    @Override
    public WalletResponseDTO getWalletDetails(UserPrincipal principal) {
        return WalletTransformer.adapt(
                walletQueryService.getWalletEntity(principal.asUser().getDbsAccountNumber()));
    }
}
