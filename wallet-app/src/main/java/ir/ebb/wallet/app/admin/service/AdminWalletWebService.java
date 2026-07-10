package ir.ebb.wallet.app.admin.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.wallet.app.admin.dto.request.CreditHistorySearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletInitCreditRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.CreditHistoryResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.RayanWalletResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletResponseDTO;
import ir.ebb.common.dto.response.PaginatedResponseDTO;

public interface AdminWalletWebService {

    void create(String userId, Long dbsAccountNumber);

    PaginatedResponseDTO<WalletResponseDTO> searchWallet(WalletSearchRequestDTO request);

    void initCredit(WalletInitCreditRequestDTO request, UserPrincipal principal);

    void removeCredit(WalletRequestDTO request, UserPrincipal principal);

    PaginatedResponseDTO<CreditHistoryResponseDTO> searchCredit(CreditHistorySearchRequestDTO request);

    RayanWalletResponseDTO getRayanWallet(long accountNumber);
}
