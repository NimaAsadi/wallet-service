package ir.ebb.wallet.service.credit.query;

import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface CreditHistoryQueryService {

    Page<CreditHistoryEntity> findAll(CreditSpecificationDTO specificationDTO, PageRequest pageRequest);
}
