package ir.ebb.wallet.service.transaction.query;

import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.entity.WalletTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface WalletTransactionQueryService {

    Page<WalletTransactionEntity> findAll(WalletTransactionSpecificationDTO spec, PageRequest pageRequest);
}
