package ir.ebb.wallet.app.user.service;

import ir.ebb.common.dto.response.PaginatedResponseDTO;
import ir.ebb.common.model.user.User;
import ir.ebb.common.utility.SecurityUtil;
import ir.ebb.wallet.app.user.dto.request.TurnoverSearchRequestDTO;
import ir.ebb.wallet.app.user.dto.response.TurnoverResponseDTO;
import ir.ebb.wallet.app.user.transformer.TurnoverTransformer;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TurnoverWebServiceImpl implements TurnoverWebService {

    private final TurnoverQueryService turnoverQueryService;

    @Override
    public List<TurnoverResponseDTO> getTodayTurnover() {
        User user = SecurityUtil.getAuthDetail().getUser();
        List<TurnoverEntity> entities = turnoverQueryService.getTodayTurnoverByUser(user);
        return TurnoverTransformer.adaptList(entities);
    }

    @Override
    public PaginatedResponseDTO<TurnoverResponseDTO> getHistory(TurnoverSearchRequestDTO request) {
        User user = SecurityUtil.getAuthDetail().getUser();
        LocalDateTime from = request.getFromCreatedAt() != null
                ? request.getFromCreatedAt().atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime to = request.getToCreatedAt() != null
                ? request.getToCreatedAt().plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        TurnoverSpecificationDTO spec = TurnoverSpecificationDTO.builder()
                .user(user)
                .fromCreatedAt(from)
                .toCreatedAt(to)
                .build();

        PageRequest pageRequest = PageRequest.of(
                request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<TurnoverEntity> page = turnoverQueryService.search(spec, pageRequest);
        return new PaginatedResponseDTO<>(page, TurnoverTransformer::adapt);
    }
}
