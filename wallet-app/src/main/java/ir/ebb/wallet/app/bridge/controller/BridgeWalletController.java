package ir.ebb.wallet.app.bridge.controller;

import ir.ebb.base.constant.ConstantTransformer;
import ir.ebb.base.dto.FixedConstantResponse;
import ir.ebb.common.controller.BaseController;
import ir.ebb.common.dto.response.BaseResponse;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.bridge.dto.request.BridgeTurnoverSearchRequestDTO;
import ir.ebb.wallet.app.bridge.dto.response.BridgeTurnoverResponseDTO;
import ir.ebb.wallet.app.bridge.dto.response.BridgeWalletResponseDTO;
import ir.ebb.wallet.app.bridge.service.BridgeTurnoverWebService;
import ir.ebb.wallet.app.bridge.service.BridgeWalletWebService;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("v1/bridge")
public class BridgeWalletController extends BaseController {

    private final BridgeWalletWebService walletWebService;
    private final BridgeTurnoverWebService turnoverWebService;

    @GetMapping("/wallet/account-number/{accountNumber}")
    public BaseResponse<BridgeWalletResponseDTO> getWalletDetails(@PathVariable Long accountNumber) {
        return success(walletWebService.getWalletDetails(accountNumber));
    }

    @GetMapping("/turnover/{accountNumber}/today")
    public BaseResponse<List<BridgeTurnoverResponseDTO>> getTodayTurnover(@PathVariable Long accountNumber) {
        return success(turnoverWebService.getTodayTurnover(accountNumber));
    }

    @GetMapping("/turnover/{accountNumber}")
    public BaseResponse<PaginatedResponseDTO<BridgeTurnoverResponseDTO>> getHistory(
            @PathVariable Long accountNumber,
            @Valid BridgeTurnoverSearchRequestDTO request) {
        return success(turnoverWebService.getHistory(accountNumber, request));
    }

    @GetMapping("/turnover/types")
    public BaseResponse<List<FixedConstantResponse>> getTypes() {
        return success(TurnoverOperationType.clientTypes.stream()
                .map(ConstantTransformer::enumToFixedConstantResponse)
                .toList());
    }
}
