package ir.ebb.wallet.service.query;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.wallet.projection.entity.WalletEntity;
import ir.ebb.wallet.valueobject.Wallet;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.dto.WalletSpecificationDTO;

import java.util.List;

public interface WalletQueryService {

    List<WalletEntity> findAll();

    Page<WalletEntity> findAll(WalletSpecificationDTO walletSpecificationDTO, PageRequest page);

    WalletEntity getWalletEntity(Long accountNumber);

    Wallet getWallet(Long accountNumber);

    boolean existsWallet(Long accountNumber);

    BuyingPower getBuyingPower(Long accountNumber, SettlementDelay settlementDelay);

    List<WalletEntity> getWalletEntities(WalletSpecificationDTO walletSpecificationDTO);

    List<WalletEntity> getSeparCreditDebtorUsers();

    void checkSeparCreditDebt(Long accountNumber);
}
