package ir.ebb.wallet.app.user.service;

import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.user.dto.request.TurnoverSearchRequestDTO;
import ir.ebb.wallet.app.user.dto.response.TurnoverResponseDTO;

import java.util.List;

public interface TurnoverWebService {

    List<TurnoverResponseDTO> getTodayTurnover();

    PaginatedResponseDTO<TurnoverResponseDTO> getHistory(TurnoverSearchRequestDTO request);
}
