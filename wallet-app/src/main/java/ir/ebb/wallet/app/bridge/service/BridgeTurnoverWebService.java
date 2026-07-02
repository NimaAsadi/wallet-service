package ir.ebb.wallet.app.bridge.service;

import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.bridge.dto.request.BridgeTurnoverSearchRequestDTO;
import ir.ebb.wallet.app.bridge.dto.response.BridgeTurnoverResponseDTO;

import java.util.List;

public interface BridgeTurnoverWebService {

    List<BridgeTurnoverResponseDTO> getTodayTurnover(Long accountNumber);

    PaginatedResponseDTO<BridgeTurnoverResponseDTO> getHistory(Long accountNumber, BridgeTurnoverSearchRequestDTO request);
}
