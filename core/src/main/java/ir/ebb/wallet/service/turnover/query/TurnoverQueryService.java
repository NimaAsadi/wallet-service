package ir.ebb.wallet.service.turnover.query;

import ir.ebb.common.model.user.User;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.TurnoverEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public interface TurnoverQueryService {

    List<TurnoverEntity> getTodayTurnoverByUser(User user);

    Page<TurnoverEntity> search(TurnoverSpecificationDTO spec, PageRequest pageRequest);

    List<Long> getAccountNumberBatch(Long lastAccountNumber, int batchSize);

    List<TurnoverEntity> getByAccountNumber(Long accountNumber);
}
