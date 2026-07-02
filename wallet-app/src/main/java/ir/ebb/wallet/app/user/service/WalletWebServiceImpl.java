package ir.ebb.wallet.app.user.service;

import ir.ebb.common.model.user.User;
import ir.ebb.common.utility.SecurityUtil;
import ir.ebb.wallet.app.user.dto.response.WalletResponseDTO;
import ir.ebb.wallet.app.user.transformer.WalletTransformer;
import ir.ebb.wallet.service.query.WalletQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WalletWebServiceImpl implements WalletWebService {

    private final WalletQueryService walletQueryService;

    @Override
    public WalletResponseDTO getWalletDetails() {
        User user = SecurityUtil.getAuthDetail().getUser();
        return WalletTransformer.adapt(
                walletQueryService.getWalletEntity(user.getDbsAccountNumber()));
    }
}
