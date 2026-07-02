package ir.ebb.wallet.app.bridge.bidardeposit.service;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.actor.WalletActorService;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletDepositRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletFreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletSpendRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletUnfreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.response.BidarDepositWalletResponseDTO;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.userinfo.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class BidarDepositWalletWebServiceImpl implements BidarDepositWalletWebService {

    private final WalletActorService walletActorService;
    private final UserQueryService userQueryService;

    @Override
    public BidarDepositWalletResponseDTO deposit(BidarDepositWalletDepositRequestDTO request) {
        User user = userQueryService.getUserByDbsAccountNumber(request.dbsAccountNumber());
        try {
            walletActorService.deposit(
                    request.trackingId(), user, request.requestAmount(),
                    SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT);
        } catch (BusinessException e) {
            log.warn("deposit failed for account {}: {}", request.dbsAccountNumber(), e.getMessage());
            throw e;
        }
        return new BidarDepositWalletResponseDTO(request.trackingId());
    }

    @Override
    public BidarDepositWalletResponseDTO freeze(BidarDepositWalletFreezeRequestDTO request) {
        User user = userQueryService.getUserByDbsAccountNumber(request.dbsAccountNumber());
        try {
            walletActorService.freeze(
                    request.trackingId(), user, request.requestAmount(),
                    SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT, false);
        } catch (BusinessException e) {
            if (e.getCode() == ExceptionConstants.INTERNAL_SERVER_ERROR.getCode()) {
                throw e; // actor timeout or infrastructure failure — don't mask as insufficient balance
            }
            throw new BusinessException("insufficient.balance", 4050);
        }
        return new BidarDepositWalletResponseDTO(request.trackingId());
    }

    @Override
    public BidarDepositWalletResponseDTO unfreeze(BidarDepositWalletUnfreezeRequestDTO request) {
        User user = userQueryService.getUserByDbsAccountNumber(request.dbsAccountNumber());
        try {
            walletActorService.unfreeze(
                    request.trackingId(), user, request.requestAmount(),
                    SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT);
        } catch (BusinessException e) {
            if (e.getCode() == ExceptionConstants.INTERNAL_SERVER_ERROR.getCode()) {
                throw e;
            }
            throw new BusinessException("insufficient.freeze", 4051);
        }
        return new BidarDepositWalletResponseDTO(request.trackingId());
    }

    @Override
    public BidarDepositWalletResponseDTO spend(BidarDepositWalletSpendRequestDTO request) {
        User user = userQueryService.getUserByDbsAccountNumber(request.dbsAccountNumber());
        try {
            walletActorService.spend(
                    request.trackingId(), user, request.requestAmount(),
                    SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT);
        } catch (BusinessException e) {
            if (e.getCode() == ExceptionConstants.INTERNAL_SERVER_ERROR.getCode()) {
                throw e;
            }
            throw new BusinessException("insufficient.freeze", 4051);
        }
        return new BidarDepositWalletResponseDTO(request.trackingId());
    }
}
