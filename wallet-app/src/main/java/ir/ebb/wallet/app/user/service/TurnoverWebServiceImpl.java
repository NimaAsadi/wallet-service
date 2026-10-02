package ir.ebb.wallet.app.user.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.user.dto.request.TurnoverSearchRequestDTO;
import ir.ebb.wallet.app.user.dto.response.TurnoverResponseDTO;
import ir.ebb.wallet.app.user.transformer.TurnoverTransformer;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TurnoverWebServiceImpl implements TurnoverWebService {

    private final TurnoverQueryService turnoverQueryService;

    @Override
    public CompletionStage<List<TurnoverResponseDTO>> getTodayTurnover(UserPrincipal principal) {
        return turnoverQueryService.getTodayTurnoverByUser(principal.asUser())
                .thenApply(TurnoverTransformer::adaptList);
    }

    @Override
    public CompletionStage<PaginatedResponseDTO<TurnoverResponseDTO>> getHistory(UserPrincipal principal, TurnoverSearchRequestDTO request) {
        LocalDateTime from = request.getFromCreatedAt() != null
                ? request.getFromCreatedAt().atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime to = request.getToCreatedAt() != null
                ? request.getToCreatedAt().plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        TurnoverSpecificationDTO spec = TurnoverSpecificationDTO.builder()
                .dbsAccountNumber(principal.asUser())
                .fromCreatedAt(from)
                .toCreatedAt(to)
                .build();

        PageRequest pageRequest = request.toPageRequest();
        return turnoverQueryService.search(spec, pageRequest)
                .thenApply(page -> new PaginatedResponseDTO<>(page, TurnoverTransformer::adapt));
    }
}
