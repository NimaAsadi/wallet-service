package ir.ebb.wallet.app.user.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.wallet.app.user.dto.request.TurnoverSearchRequestDTO;
import ir.ebb.wallet.app.user.dto.response.TurnoverResponseDTO;
import ir.ebb.common.dto.response.PaginatedResponseDTO;

import java.util.concurrent.CompletionStage;

public interface TurnoverWebService {

    CompletionStage<java.util.List<TurnoverResponseDTO>> getTodayTurnover(UserPrincipal principal);

    CompletionStage<PaginatedResponseDTO<TurnoverResponseDTO>> getHistory(UserPrincipal principal, TurnoverSearchRequestDTO request);
}
