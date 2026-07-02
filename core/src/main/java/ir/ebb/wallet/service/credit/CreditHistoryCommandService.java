package ir.ebb.wallet.service.credit;

import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

public interface CreditHistoryCommandService {

    CreditHistoryEntity save(CreditHistoryEntity entity);

    void updateStatus(UUID id, RayanCreditStatus status, String errorMessage);

    Page<CreditHistoryEntity> search(CreditSpecificationDTO spec, PageRequest pageRequest);
}
