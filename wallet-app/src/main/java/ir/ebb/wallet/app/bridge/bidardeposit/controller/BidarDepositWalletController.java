package ir.ebb.wallet.app.bridge.bidardeposit.controller;

import ir.ebb.common.controller.BaseController;
import ir.ebb.common.dto.response.BaseResponse;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletDepositRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletFreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletSpendRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletUnfreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.response.BidarDepositWalletResponseDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.service.BidarDepositWalletWebService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("v1/bridge/bidar-deposit")
public class BidarDepositWalletController extends BaseController {

    private final BidarDepositWalletWebService bidarDepositWalletWebService;

    @PostMapping("deposit")
    public BaseResponse<BidarDepositWalletResponseDTO> deposit(
            @Valid @RequestBody BidarDepositWalletDepositRequestDTO request) {
        return success(bidarDepositWalletWebService.deposit(request));
    }

    @PostMapping("freeze")
    public BaseResponse<BidarDepositWalletResponseDTO> freeze(
            @Valid @RequestBody BidarDepositWalletFreezeRequestDTO request) {
        return success(bidarDepositWalletWebService.freeze(request));
    }

    @PostMapping("unfreeze")
    public BaseResponse<BidarDepositWalletResponseDTO> unfreeze(
            @Valid @RequestBody BidarDepositWalletUnfreezeRequestDTO request) {
        return success(bidarDepositWalletWebService.unfreeze(request));
    }

    @PostMapping("withdraw")
    public BaseResponse<BidarDepositWalletResponseDTO> spend(
            @Valid @RequestBody BidarDepositWalletSpendRequestDTO request) {
        return success(bidarDepositWalletWebService.spend(request));
    }
}
