package ir.ebb.wallet.app.admin.service;

import ir.ebb.wallet.app.admin.dto.request.WalletTransactionSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletTransactionResponseDTO;
import ir.ebb.common.dto.response.PaginatedResponseDTO;

public interface AdminWalletTransactionWebService {

    PaginatedResponseDTO<WalletTransactionResponseDTO> searchWalletTransaction(WalletTransactionSearchRequestDTO request);
}
