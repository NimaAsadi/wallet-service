package ir.ebb.wallet.app.admin.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.admin.dto.request.CreditHistorySearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletInitCreditRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.CreditHistoryResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.RayanWalletResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletResponseDTO;

import java.util.concurrent.CompletionStage;

public interface AdminWalletWebService {

    CompletionStage<Void> create(String userId, Long dbsAccountNumber);

    CompletionStage<PaginatedResponseDTO<WalletResponseDTO>> searchWallet(WalletSearchRequestDTO request);

    CompletionStage<Void> initCredit(WalletInitCreditRequestDTO request, UserPrincipal principal);

    CompletionStage<Void> removeCredit(WalletRequestDTO request, UserPrincipal principal);

    CompletionStage<PaginatedResponseDTO<CreditHistoryResponseDTO>> searchCredit(CreditHistorySearchRequestDTO request);

    /** Sync on purpose — the Rayan client + repos block (HTTP + JDBC). */
    RayanWalletResponseDTO getRayanWallet(long accountNumber);
}
