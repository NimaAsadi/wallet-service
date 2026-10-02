package ir.ebb.wallet.service.turnover.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.TurnoverSpecificationDTO;
import ir.ebb.wallet.projection.entity.TurnoverEntity;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface TurnoverQueryService {

    CompletionStage<List<TurnoverEntity>> getTodayTurnoverByUser(long dbsAccountNumber);

    CompletionStage<Page<TurnoverEntity>> search(TurnoverSpecificationDTO spec, PageRequest pageRequest);

    CompletionStage<List<Long>> getAccountNumberBatch(Long lastAccountNumber, int batchSize);

    CompletionStage<List<TurnoverEntity>> getByAccountNumber(Long accountNumber);
}
