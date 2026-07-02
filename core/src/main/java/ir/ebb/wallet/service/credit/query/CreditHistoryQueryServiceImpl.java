package ir.ebb.wallet.service.credit.query;

import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import ir.ebb.wallet.repository.credit.CreditHistoryRepository;
import ir.ebb.wallet.repository.credit.CreditHistorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CreditHistoryQueryServiceImpl implements CreditHistoryQueryService {

    private final CreditHistoryRepository creditHistoryRepository;

    @Override
    public Page<CreditHistoryEntity> findAll(CreditSpecificationDTO specificationDTO, PageRequest pageRequest) {
        return creditHistoryRepository.findAll(CreditHistorySpecification.search(specificationDTO), pageRequest);
    }
}
