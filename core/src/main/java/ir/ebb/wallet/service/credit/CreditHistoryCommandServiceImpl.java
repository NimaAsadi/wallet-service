package ir.ebb.wallet.service.credit;

import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import ir.ebb.wallet.dto.CreditSpecificationDTO;
import ir.ebb.wallet.entity.CreditHistoryEntity;
import ir.ebb.wallet.repository.credit.CreditHistoryRepository;
import ir.ebb.wallet.repository.credit.CreditHistorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CreditHistoryCommandServiceImpl implements CreditHistoryCommandService {

    private final CreditHistoryRepository creditHistoryRepository;

    @Override
    public CreditHistoryEntity save(CreditHistoryEntity entity) {
        return creditHistoryRepository.save(entity);
    }

    @Override
    public void updateStatus(UUID id, RayanCreditStatus status, String errorMessage) {
        creditHistoryRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setErrorMessage(errorMessage);
            creditHistoryRepository.save(entity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CreditHistoryEntity> search(CreditSpecificationDTO spec, PageRequest pageRequest) {
        return creditHistoryRepository.findAll(CreditHistorySpecification.search(spec), pageRequest);
    }
}
