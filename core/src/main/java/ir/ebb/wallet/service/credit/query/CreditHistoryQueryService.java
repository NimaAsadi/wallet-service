package ir.ebb.wallet.service.credit.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.entity.CreditHistoryEntity;

public interface CreditHistoryQueryService {

    Page<CreditHistoryEntity> findAll(CreditSpecificationDTO specificationDTO, PageRequest pageRequest);
}
