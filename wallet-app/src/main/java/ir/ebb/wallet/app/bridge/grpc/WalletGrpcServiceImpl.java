package ir.ebb.wallet.app.bridge.grpc;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.grpc.BuyingPowerResponse;
import ir.ebb.wallet.grpc.GetBuyingPowerRequest;
import ir.ebb.wallet.grpc.GetWalletRequest;
import ir.ebb.wallet.grpc.WalletResponse;
import ir.ebb.wallet.grpc.WalletServiceGrpc;
import ir.ebb.wallet.wallet.WalletFacade;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WalletGrpcServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletFacade walletFacade;

    @Override
    public void getWallet(GetWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        try {
            Wallet wallet = walletFacade.getWallet(request.getAccountNumber());
            long balance = wallet.getT0().getBalance() + wallet.getT1().getBalance() + wallet.getT2().getBalance();
            long frozen = wallet.getT0().getFrozen() + wallet.getT1().getFrozen() + wallet.getT2().getFrozen();

            WalletResponse response = WalletResponse.newBuilder()
                    .setBalance(balance)
                    .setT0(wallet.getT0().getBalance())
                    .setT1(wallet.getT1().getBalance() + wallet.getT0().getBalance())
                    .setT2(wallet.getT2().getBalance() + wallet.getT1().getBalance() + wallet.getT0().getBalance())
                    .setTotalFrozen(frozen)
                    .setCredit(wallet.getCredit())
                    .setInitCredit(wallet.getInitialCredit())
                    .setSeparCredit(wallet.getSeparCredit())
                    .setSeparInitCredit(wallet.getSeparInitialCredit())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.atError().log("gRPC getWallet failed for account={}: {}", request.getAccountNumber(), e.getMessage());
            responseObserver.onError(toStatus(e)
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getBuyingPower(GetBuyingPowerRequest request, StreamObserver<BuyingPowerResponse> responseObserver) {
        try {
            SettlementDelay settlementDelay = SettlementDelay.valueOf(request.getSettlementDelay());
            BuyingPower bp = walletFacade.getBuyingPower(request.getAccountNumber(), settlementDelay);

            BuyingPowerResponse response = BuyingPowerResponse.newBuilder()
                    .setBalance(bp.balance())
                    .setCredit(bp.credit())
                    .setSeparCredit(bp.separCredit())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.atError().log("gRPC getBuyingPower failed for account={}: {}", request.getAccountNumber(), e.getMessage());
            responseObserver.onError(toStatus(e)
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    /** WALLET_NOT_EXIST (missing wallet) → NOT_FOUND; anything else (e.g. actor unavailable) → INTERNAL. */
    private static io.grpc.Status toStatus(Throwable e) {
        if (e instanceof BusinessException be && be.getCode() != null
                && be.getCode() == ExceptionConstants.WALLET_NOT_EXIST.getCode()) {
            return io.grpc.Status.NOT_FOUND;
        }
        return io.grpc.Status.INTERNAL;
    }
}
