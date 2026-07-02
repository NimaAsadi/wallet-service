package ir.ebb.wallet.app.user.controller;

import ir.ebb.common.controller.BaseController;
import ir.ebb.common.dto.response.BaseResponse;
import ir.ebb.wallet.app.user.dto.response.WalletResponseDTO;
import ir.ebb.wallet.app.user.service.WalletWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("v1/user/wallet")
public class WalletController extends BaseController {

    private final WalletWebService walletWebService;

    @GetMapping
    public BaseResponse<WalletResponseDTO> getWalletDetails() {
        return success(walletWebService.getWalletDetails());
    }
}
