package ir.ebb.wallet.service.transaction.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.entity.WalletTransactionEntity;

public interface WalletTransactionQueryService {

    Page<WalletTransactionEntity> findAll(WalletTransactionSpecificationDTO spec, PageRequest pageRequest);
}
