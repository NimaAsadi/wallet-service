package ir.ebb.wallet.service.turnover.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.repository.turnover.TurnoverRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class TurnoverQueryServiceImpl implements TurnoverQueryService {

    private final TurnoverRepository turnoverRepository;

    @Override
    public List<TurnoverEntity> getTodayTurnoverByUser(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        TurnoverSpecificationDTO spec = TurnoverSpecificationDTO.builder()
                .user(user)
                .fromCreatedAt(startOfDay)
                .toCreatedAt(endOfDay)
                .build();
        return turnoverRepository.findAll(spec);
    }

    @Override
    public Page<TurnoverEntity> search(TurnoverSpecificationDTO spec, PageRequest pageRequest) {
        return turnoverRepository.findAll(spec, pageRequest);
    }

    @Override
    public List<Long> getAccountNumberBatch(Long lastAccountNumber, int batchSize) {
        return turnoverRepository.findAccountNumberBatch(lastAccountNumber, batchSize);
    }

    @Override
    public List<TurnoverEntity> getByAccountNumber(Long accountNumber) {
        return turnoverRepository.findByAccountNumber(accountNumber);
    }
}
