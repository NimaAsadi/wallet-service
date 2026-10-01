package ir.ebb.wallet.service.credit.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.projection.entity.CreditHistoryEntity;
import ir.ebb.wallet.projection.repository.CreditHistoryRepository;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CreditHistoryQueryServiceImpl implements CreditHistoryQueryService {

    private final CreditHistoryRepository creditHistoryRepository;

    @Override
    public Page<CreditHistoryEntity> findAll(CreditSpecificationDTO specificationDTO, PageRequest pageRequest) {
        return creditHistoryRepository.findAll(specificationDTO, pageRequest);
    }
}
