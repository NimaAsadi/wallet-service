package ir.ebb.wallet.app.admin.service;

import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletTransactionSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletTransactionResponseDTO;
import ir.ebb.wallet.app.admin.transformer.WalletTransactionTransformer;
import ir.ebb.wallet.service.transaction.query.WalletTransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AdminWalletTransactionWebServiceImpl implements AdminWalletTransactionWebService {

    private final WalletTransactionQueryService walletTransactionQueryService;

    @Override
    public PaginatedResponseDTO<WalletTransactionResponseDTO> searchWalletTransaction(WalletTransactionSearchRequestDTO request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), Sort.by(request.getOrderBy()));
        return new PaginatedResponseDTO<>(
                walletTransactionQueryService.findAll(WalletTransactionTransformer.adapt(request), pageRequest),
                WalletTransactionTransformer::adapt
        );
    }
}
