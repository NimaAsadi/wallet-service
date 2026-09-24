package ir.ebb.wallet.service.turnover.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.entity.TurnoverEntity;

import java.util.List;

public interface TurnoverQueryService {

    List<TurnoverEntity> getTodayTurnoverByUser(long dbsAccountNumber);

    Page<TurnoverEntity> search(TurnoverSpecificationDTO spec, PageRequest pageRequest);

    List<Long> getAccountNumberBatch(Long lastAccountNumber, int batchSize);

    List<TurnoverEntity> getByAccountNumber(Long accountNumber);
}
