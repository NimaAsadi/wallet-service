package ir.ebb.wallet.service.transaction.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.WalletTransactionSpecificationDTO;
import ir.ebb.wallet.projection.entity.WalletTransactionEntity;

import java.util.concurrent.CompletionStage;

public interface WalletTransactionQueryService {

    CompletionStage<Page<WalletTransactionEntity>> findAll(WalletTransactionSpecificationDTO spec, PageRequest pageRequest);
}
