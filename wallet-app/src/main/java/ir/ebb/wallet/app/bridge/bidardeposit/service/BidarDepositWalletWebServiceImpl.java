package ir.ebb.wallet.app.bridge.bidardeposit.service;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletDepositRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletFreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletSpendRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletUnfreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.response.BidarDepositWalletResponseDTO;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.dto.DepositDTO;
import ir.ebb.wallet.dto.FreezeDTO;
import ir.ebb.wallet.dto.SpendDTO;
import ir.ebb.wallet.dto.UnfreezeDTO;
import ir.ebb.wallet.service.WalletErrors;
import ir.ebb.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Bidar-deposit writes through the sharded entity. The entity replies {@code StatusReply};
 * failures surface as failed stages — the error mapping that used to be a sync try/catch runs in
 * {@code handle}, with {@link WalletErrors#rootCause(Throwable)} unwrapping the
 * {@code CompletionException} layers first.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BidarDepositWalletWebServiceImpl implements BidarDepositWalletWebService {

    private final WalletService walletService;

    @Override
    public CompletionStage<BidarDepositWalletResponseDTO> deposit(BidarDepositWalletDepositRequestDTO request) {
        return walletService.deposit(new DepositDTO(
                        request.dbsAccountNumber(), request.trackingId(), request.requestAmount(),
                        SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT))
                .handle((done, error) -> {
                    if (error == null) return new BidarDepositWalletResponseDTO(request.trackingId());
                    log.atWarn().log("deposit failed for account {}: {}",
                            request.dbsAccountNumber(), WalletErrors.rootCause(error).getMessage());
                    throw WalletErrors.rootCause(error);
                });
    }

    @Override
    public CompletionStage<BidarDepositWalletResponseDTO> freeze(BidarDepositWalletFreezeRequestDTO request) {
        return walletService.freeze(new FreezeDTO(
                        request.dbsAccountNumber(), request.trackingId(), request.requestAmount(),
                        SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT, false))
                .handle((done, error) -> {
                    if (error == null) return new BidarDepositWalletResponseDTO(request.trackingId());
                    // Actor timeout / infrastructure failure keeps its own code; anything else is
                    // reported to the bridge as insufficient balance.
                    throw translate(error, "insufficient.balance", 4050);
                });
    }

    @Override
    public CompletionStage<BidarDepositWalletResponseDTO> unfreeze(BidarDepositWalletUnfreezeRequestDTO request) {
        return walletService.unfreeze(new UnfreezeDTO(
                        request.dbsAccountNumber(), request.trackingId(), request.requestAmount(),
                        SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT, false))
                .handle((done, error) -> {
                    if (error == null) return new BidarDepositWalletResponseDTO(request.trackingId());
                    throw translate(error, "insufficient.freeze", 4051);
                });
    }

    @Override
    public CompletionStage<BidarDepositWalletResponseDTO> spend(BidarDepositWalletSpendRequestDTO request) {
        return walletService.spend(new SpendDTO(
                        request.dbsAccountNumber(), request.trackingId(), request.requestAmount(),
                        SettlementDelay.T_PLUS_0, WalletTransactionType.BIDAR_DEPOSIT, false))
                .handle((done, error) -> {
                    if (error == null) return new BidarDepositWalletResponseDTO(request.trackingId());
                    throw translate(error, "insufficient.freeze", 4051);
                });
    }

    /**
     * Keeps genuine infrastructure failures (5000 — actor timeout, shard unavailable) as they are;
     * maps every other domain rejection to the bridge's coarse insufficient code.
     */
    private static BusinessException translate(Throwable error, String message, int insufficientCode) {
        Throwable cause = WalletErrors.rootCause(error);
        if (cause instanceof BusinessException be) {
            if (Objects.equals(be.getCode(), ExceptionConstants.INTERNAL_SERVER_ERROR.getCode())) {
                return be;
            }
        }
        return new BusinessException(message, insufficientCode);
    }
}
