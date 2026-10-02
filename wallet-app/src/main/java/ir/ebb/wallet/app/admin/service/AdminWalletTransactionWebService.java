package ir.ebb.wallet.app.admin.service;

import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletTransactionSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletTransactionResponseDTO;

import java.util.concurrent.CompletionStage;

public interface AdminWalletTransactionWebService {

    CompletionStage<PaginatedResponseDTO<WalletTransactionResponseDTO>> searchWalletTransaction(WalletTransactionSearchRequestDTO request);
}
