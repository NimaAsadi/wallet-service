package ir.ebb.wallet.app.admin.controller;

import ir.ebb.base.constant.ConstantTransformer;
import ir.ebb.base.dto.FixedConstantResponse;
import ir.ebb.common.controller.BaseController;
import ir.ebb.common.dto.response.BaseResponse;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.admin.dto.request.CreditHistorySearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletInitCreditRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.response.CreditHistoryResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.RayanWalletResponseDTO;
import ir.ebb.wallet.app.admin.dto.response.WalletResponseDTO;
import ir.ebb.wallet.app.admin.service.AdminWalletWebService;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("v1/admin/wallet")
public class AdminWalletController extends BaseController {

    private final AdminWalletWebService walletService;

    @PreAuthorize("hasAuthority('PERMISSION_USERS_UPDATE')")
    @PostMapping
    public BaseResponse<Void> create(@RequestBody @Valid WalletRequestDTO request) {
        walletService.create(request.userId(), request.dbsAccountNumber());
        return success(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('PERMISSION_CUSTOMERWALLET_VIEW')")
    @GetMapping
    public BaseResponse<PaginatedResponseDTO<WalletResponseDTO>> searchWallet(WalletSearchRequestDTO request) {
        return success(walletService.searchWallet(request));
    }

    @PreAuthorize("hasAuthority('PERMISSION_CUSTOMERWALLET_VIEW')")
    @GetMapping("/rayan/{accountNumber}")
    public BaseResponse<RayanWalletResponseDTO> getRayanWallet(@PathVariable Long accountNumber) {
        return success(walletService.getRayanWallet(accountNumber));
    }

    @PreAuthorize("hasAuthority('PERMISSION_CREDITS_ADD') and hasAuthority('PERMISSION_USERS_SEARCH')")
    @PutMapping("/credit/init")
    public BaseResponse<Void> initCredit(@RequestBody @Valid WalletInitCreditRequestDTO request) {
        walletService.initCredit(request);
        return success(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('PERMISSION_CREDITS_UPDATE') and hasAuthority('PERMISSION_USERS_SEARCH')")
    @PutMapping("/credit/remove")
    public BaseResponse<Void> removeCredit(@RequestBody @Valid WalletRequestDTO request) {
        walletService.removeCredit(request);
        return success(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('PERMISSION_CREDITS_VIEW') and hasAuthority('PERMISSION_USERS_SEARCH')")
    @GetMapping("/credit/history")
    public BaseResponse<PaginatedResponseDTO<CreditHistoryResponseDTO>> getCreditHistory(CreditHistorySearchRequestDTO request) {
        return success(walletService.searchCredit(request));
    }

    @PreAuthorize("hasAuthority('PERMISSION_CREDITS_VIEW')")
    @GetMapping("/credit/status")
    public BaseResponse<List<FixedConstantResponse>> getStatuses() {
        return success(Stream.of(RayanCreditStatus.values())
                .map(ConstantTransformer::enumToFixedConstantResponse)
                .toList());
    }
}
