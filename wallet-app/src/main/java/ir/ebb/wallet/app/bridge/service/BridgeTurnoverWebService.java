package ir.ebb.wallet.app.bridge.service;

import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.bridge.dto.request.BridgeTurnoverSearchRequestDTO;
import ir.ebb.wallet.app.bridge.dto.response.BridgeTurnoverResponseDTO;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface BridgeTurnoverWebService {

    CompletionStage<List<BridgeTurnoverResponseDTO>> getTodayTurnover(Long accountNumber);

    CompletionStage<PaginatedResponseDTO<BridgeTurnoverResponseDTO>> getHistory(Long accountNumber, BridgeTurnoverSearchRequestDTO request);
}
