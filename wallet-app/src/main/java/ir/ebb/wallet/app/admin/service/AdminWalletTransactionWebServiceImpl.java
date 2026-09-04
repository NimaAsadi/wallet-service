package ir.ebb.wallet.app.admin.service;

import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletTransactionSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletTransactionResponseDTO;
import ir.ebb.wallet.app.admin.transformer.WalletTransactionTransformer;
import ir.ebb.wallet.service.transaction.query.WalletTransactionQueryService;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminWalletTransactionWebServiceImpl implements AdminWalletTransactionWebService {

    private final WalletTransactionQueryService walletTransactionQueryService;

    @Override
    public PaginatedResponseDTO<WalletTransactionResponseDTO> searchWalletTransaction(WalletTransactionSearchRequestDTO request) {
        return new PaginatedResponseDTO<>(
                walletTransactionQueryService.findAll(WalletTransactionTransformer.adapt(request), request.toPageRequest()),
                WalletTransactionTransformer::adapt
        );
    }
}
