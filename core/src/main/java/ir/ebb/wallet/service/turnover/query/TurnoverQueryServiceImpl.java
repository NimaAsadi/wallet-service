package ir.ebb.wallet.service.turnover.query;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.TurnoverEntity;
import ir.ebb.wallet.repository.turnover.TurnoverRepository;
import ir.ebb.wallet.repository.turnover.TurnoverSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
        return turnoverRepository.findAll(TurnoverSpecification.search(spec));
    }

    @Override
    public Page<TurnoverEntity> search(TurnoverSpecificationDTO spec, PageRequest pageRequest) {
        return turnoverRepository.findAll(TurnoverSpecification.search(spec), pageRequest);
    }

    @Override
    public List<Long> getAccountNumberBatch(Long lastAccountNumber, int batchSize) {
        return turnoverRepository.findAccountNumberBatch(lastAccountNumber, PageRequest.of(0, batchSize));
    }

    @Override
    public List<TurnoverEntity> getByAccountNumber(Long accountNumber) {
        return turnoverRepository.findByAccountNumber(accountNumber);
    }
}
