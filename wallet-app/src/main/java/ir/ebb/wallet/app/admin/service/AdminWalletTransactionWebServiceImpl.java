package ir.ebb.wallet.app.admin.service;

import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletTransactionSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletTransactionResponseDTO;
import ir.ebb.wallet.app.admin.transformer.WalletTransactionTransformer;
import ir.ebb.wallet.service.transaction.query.WalletTransactionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminWalletTransactionWebServiceImpl implements AdminWalletTransactionWebService {

    private final WalletTransactionQueryService walletTransactionQueryService;

    @Override
    public CompletionStage<PaginatedResponseDTO<WalletTransactionResponseDTO>> searchWalletTransaction(WalletTransactionSearchRequestDTO request) {
        return walletTransactionQueryService
                .findAll(WalletTransactionTransformer.adapt(request), request.toPageRequest())
                .thenApply(page -> new PaginatedResponseDTO<>(page, WalletTransactionTransformer::adapt));
    }
}
