package ir.ebb.wallet.app.user.controller;

import ir.ebb.base.constant.ConstantTransformer;
import ir.ebb.base.dto.FixedConstantResponse;
import ir.ebb.common.controller.BaseController;
import ir.ebb.common.dto.response.BaseResponse;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.user.dto.request.TurnoverSearchRequestDTO;
import ir.ebb.wallet.app.user.dto.response.TurnoverResponseDTO;
import ir.ebb.wallet.app.user.service.TurnoverWebService;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("v1/user/turnover")
public class TurnoverController extends BaseController {

    private final TurnoverWebService turnoverWebService;

    @GetMapping("/today")
    public BaseResponse<List<TurnoverResponseDTO>> getTodayTurnover() {
        return success(turnoverWebService.getTodayTurnover());
    }

    @GetMapping
    public BaseResponse<PaginatedResponseDTO<TurnoverResponseDTO>> getHistory(@Valid TurnoverSearchRequestDTO request) {
        return success(turnoverWebService.getHistory(request));
    }

    @GetMapping("/types")
    public BaseResponse<List<FixedConstantResponse>> getTypes() {
        return success(TurnoverOperationType.clientTypes.stream()
                .map(ConstantTransformer::enumToFixedConstantResponse).toList());
    }
}
