package ir.ebb.wallet.service.transaction.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.entity.WalletTransactionEntity;
import ir.ebb.wallet.repository.transaction.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WalletTransactionQueryServiceImpl implements WalletTransactionQueryService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public Page<WalletTransactionEntity> findAll(WalletTransactionSpecificationDTO spec, PageRequest pageRequest) {
        return walletTransactionRepository.findAll(spec, pageRequest);
    }
}
