package ir.ebb.wallet.service.query;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.dto.WalletSpecificationDTO;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.repository.WalletRepository;
import ir.ebb.wallet.repository.WalletSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletQueryServiceImpl implements WalletQueryService {

    private final WalletRepository walletRepository;

    @Override
    public List<WalletEntity> findAll() {
        return walletRepository.findAll();
    }

    @Override
    public Page<WalletEntity> findAll(WalletSpecificationDTO dto, PageRequest page) {
        return walletRepository.findAll(WalletSpecification.search(dto), page);
    }

    @Override
    public WalletEntity getWalletEntity(Long dbsAccountNumber) {
        return walletRepository.findByUser_DbsAccountNumber(dbsAccountNumber)
                .orElseThrow(() -> new BusinessException(
                        ExceptionConstants.WALLET_NOT_EXIST.getMessage(),
                        ExceptionConstants.WALLET_NOT_EXIST.getCode()));
    }

    @Override
    public Wallet getWallet(Long dbsAccountNumber) {
        return getWalletEntity(dbsAccountNumber).adaptToDomain();
    }

    @Override
    public boolean existsWallet(User user) {
        return walletRepository.findFirstByUser(user).isPresent();
    }

    @Override
    public BuyingPower getBuyingPower(long dbsAccountNumber, SettlementDelay settlementDelay) {
        return getWalletEntity(dbsAccountNumber).adaptToDomain().buyingPower(settlementDelay);
    }

    @Override
    public List<WalletEntity> getWalletEntities(WalletSpecificationDTO dto) {
        return walletRepository.findAll(WalletSpecification.search(dto));
    }

    @Override
    public List<WalletEntity> getSeparCreditDebtorUsers() {
        return walletRepository.findSeparCreditDebtors();
    }

    @Override
    public void checkSeparCreditDebt(User user) {
        if (walletRepository.existsByUserAndSeparCreditLessThanSeparInitialCredit(user)) {
            throw new BusinessException(ExceptionConstants.SEPAR_CREDIT_DEBT);
        }
    }
}
