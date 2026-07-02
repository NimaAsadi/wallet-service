package ir.ebb.wallet.app.bridge.service;

import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;
import ir.ebb.wallet.app.bridge.transformer.BridgeWalletTransformer;
import ir.ebb.wallet.service.query.WalletQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class BridgeWalletWebServiceImpl implements BridgeWalletWebService {

    private final WalletQueryService walletQueryService;

    @Override
    public BridgeWalletResponseDTO getWalletDetails(Long accountNumber) {
        return BridgeWalletTransformer.adapt(walletQueryService.getWalletEntity(accountNumber));
    }
}
