package ir.ebb.wallet.app.admin.controller;

import ir.ebb.common.controller.BaseController;
import ir.ebb.common.dto.response.BaseResponse;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletTransactionSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletTransactionResponseDTO;
import ir.ebb.wallet.app.admin.service.AdminWalletTransactionWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("v1/admin/wallet-transaction")
public class AdminWalletTransactionController extends BaseController {

    private final AdminWalletTransactionWebService walletTransactionWebService;

    @PreAuthorize("hasAuthority('PERMISSION_CREDITS_VIEW')")
    @GetMapping
    public BaseResponse<PaginatedResponseDTO<WalletTransactionResponseDTO>> searchWalletTransaction(
            WalletTransactionSearchRequestDTO request) {
        return success(walletTransactionWebService.searchWalletTransaction(request));
    }
}
