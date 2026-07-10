package ir.ebb.wallet.app.user.service;

import ir.ebb.base.security.UserPrincipal;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.wallet.app.user.dto.request.TurnoverSearchRequestDTO;
import ir.ebb.wallet.app.user.dto.response.TurnoverResponseDTO;
import ir.ebb.wallet.app.user.transformer.TurnoverTransformer;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class TurnoverWebServiceImpl implements TurnoverWebService {

    private final TurnoverQueryService turnoverQueryService;

    @Override
    public List<TurnoverResponseDTO> getTodayTurnover(UserPrincipal principal) {
        List<TurnoverEntity> entities = turnoverQueryService.getTodayTurnoverByUser(principal.asUser());
        return TurnoverTransformer.adaptList(entities);
    }

    @Override
    public PaginatedResponseDTO<TurnoverResponseDTO> getHistory(UserPrincipal principal, TurnoverSearchRequestDTO request) {
        LocalDateTime from = request.getFromCreatedAt() != null
                ? request.getFromCreatedAt().atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime to = request.getToCreatedAt() != null
                ? request.getToCreatedAt().plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        TurnoverSpecificationDTO spec = TurnoverSpecificationDTO.builder()
                .user(principal.asUser())
                .fromCreatedAt(from)
                .toCreatedAt(to)
                .build();

        PageRequest pageRequest = request.toPageRequest();
        Page<TurnoverEntity> page = turnoverQueryService.search(spec, pageRequest);
        return new PaginatedResponseDTO<>(page, TurnoverTransformer::adapt);
    }
}
