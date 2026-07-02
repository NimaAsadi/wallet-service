package ir.ebb.wallet.service.transaction.query;

import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.entity.WalletTransactionEntity;
import ir.ebb.wallet.repository.transaction.WalletTransactionRepository;
import ir.ebb.wallet.repository.transaction.WalletTransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WalletTransactionQueryServiceImpl implements WalletTransactionQueryService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public Page<WalletTransactionEntity> findAll(WalletTransactionSpecificationDTO spec, PageRequest pageRequest) {
        return walletTransactionRepository.findAll(WalletTransactionSpecification.search(spec), pageRequest);
    }
}
