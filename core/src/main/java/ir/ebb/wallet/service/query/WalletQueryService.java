package ir.ebb.wallet.service.query;

import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.projection.entity.WalletEntity;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Bulk/list reads over the {@code wallet} read-model tables (blocking JDBC on the blocking
 * dispatcher). Single-wallet reads are NOT here — {@code WalletService.getWallet/getBuyingPower}
 * serves those strongly-consistent from the sharded entity.
 */
public interface WalletQueryService {

    CompletionStage<List<WalletEntity>> findAll();

    CompletionStage<Page<WalletEntity>> findAll(WalletSpecificationDTO walletSpecificationDTO, PageRequest page);

    CompletionStage<WalletEntity> getWalletEntity(Long accountNumber);

    CompletionStage<Boolean> existsWallet(Long accountNumber);

    CompletionStage<List<WalletEntity>> getWalletEntities(WalletSpecificationDTO walletSpecificationDTO);

    CompletionStage<List<WalletEntity>> getSeparCreditDebtorUsers();

    CompletionStage<Void> checkSeparCreditDebt(Long accountNumber);
}
