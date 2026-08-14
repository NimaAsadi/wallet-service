package ir.ebb.wallet.service.query;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.dto.request.PageRequest;
import ir.ebb.common.dto.response.Page;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.valueobject.Wallet;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.entity.WalletEntity;

import java.util.List;

public interface WalletQueryService {

    List<WalletEntity> findAll();

    Page<WalletEntity> findAll(WalletSpecificationDTO walletSpecificationDTO, PageRequest page);

    WalletEntity getWalletEntity(Long dbsAccountNumber);

    Wallet getWallet(Long dbsAccountNumber);

    boolean existsWallet(User user);

    BuyingPower getBuyingPower(long dbsAccountNumber, SettlementDelay settlementDelay);

    List<WalletEntity> getWalletEntities(WalletSpecificationDTO walletSpecificationDTO);

    List<WalletEntity> getSeparCreditDebtorUsers();

    void checkSeparCreditDebt(User user);
}
