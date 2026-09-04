package ir.ebb.wallet.app.bridge.bidardeposit.service;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.wallet.WalletFacade;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletDepositRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletFreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletSpendRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletUnfreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.response.BidarDepositWalletResponseDTO;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BidarDepositWalletWebServiceImpl implements BidarDepositWalletWebService {

    private final WalletFacade walletFacade;

    @Override
    public BidarDepositWalletResponseDTO deposit(BidarDepositWalletDepositRequestDTO request) {
        try {
            walletFacade.deposit(
                    request.trackingId(), request.dbsAccountNumber(), request.requestAmount(),
                    SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT);
        } catch (BusinessException e) {
            log.warn("deposit failed for account {}: {}", request.dbsAccountNumber(), e.getMessage());
            throw e;
        }
        return new BidarDepositWalletResponseDTO(request.trackingId());
    }

    @Override
    public BidarDepositWalletResponseDTO freeze(BidarDepositWalletFreezeRequestDTO request) {
        try {
            walletFacade.freeze(
                    request.trackingId(), request.dbsAccountNumber(), request.requestAmount(),
                    SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT, false);
        } catch (BusinessException e) {
            if (Objects.equals(e.getCode(), ExceptionConstants.INTERNAL_SERVER_ERROR.getCode())) {
                throw e; // actor timeout or infrastructure failure — don't mask as insufficient balance
            }
            throw new BusinessException("insufficient.balance", 4050);
        }
        return new BidarDepositWalletResponseDTO(request.trackingId());
    }

    @Override
    public BidarDepositWalletResponseDTO unfreeze(BidarDepositWalletUnfreezeRequestDTO request) {
        try {
            walletFacade.unfreeze(
                    request.trackingId(), request.dbsAccountNumber(), request.requestAmount(),
                    SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT);
        } catch (BusinessException e) {
            if (Objects.equals(e.getCode(), ExceptionConstants.INTERNAL_SERVER_ERROR.getCode())) {
                throw e;
            }
            throw new BusinessException("insufficient.freeze", 4051);
        }
        return new BidarDepositWalletResponseDTO(request.trackingId());
    }

    @Override
    public BidarDepositWalletResponseDTO spend(BidarDepositWalletSpendRequestDTO request) {
        try {
            walletFacade.spend(
                    request.trackingId(), request.dbsAccountNumber(), request.requestAmount(),
                    SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT);
        } catch (BusinessException e) {
            if (Objects.equals(e.getCode(), ExceptionConstants.INTERNAL_SERVER_ERROR.getCode())) {
                throw e;
            }
            throw new BusinessException("insufficient.freeze", 4051);
        }
        return new BidarDepositWalletResponseDTO(request.trackingId());
    }
}
