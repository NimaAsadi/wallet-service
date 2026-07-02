package ir.ebb.wallet.app.bridge.grpc;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.grpc.BuyingPowerResponse;
import ir.ebb.wallet.grpc.GetBuyingPowerRequest;
import ir.ebb.wallet.grpc.GetWalletRequest;
import ir.ebb.wallet.grpc.WalletResponse;
import ir.ebb.wallet.grpc.WalletServiceGrpc;
import ir.ebb.wallet.service.query.WalletQueryService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class WalletGrpcServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletQueryService walletQueryService;

    @Override
    public void getWallet(GetWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        try {
            WalletEntity entity = walletQueryService.getWalletEntity(request.getAccountNumber());
            long balance = entity.getT0().getBalance() + entity.getT1().getBalance() + entity.getT2().getBalance();
            long frozen = entity.getT0().getFrozen() + entity.getT1().getFrozen() + entity.getT2().getFrozen();

            WalletResponse response = WalletResponse.newBuilder()
                    .setBalance(balance)
                    .setT0(entity.getT0().getBalance())
                    .setT1(entity.getT1().getBalance() + entity.getT0().getBalance())
                    .setT2(entity.getT2().getBalance() + entity.getT1().getBalance() + entity.getT0().getBalance())
                    .setTotalFrozen(frozen)
                    .setCredit(entity.getCredit())
                    .setInitCredit(entity.getInitialCredit())
                    .setSeparCredit(entity.getSeparCredit())
                    .setSeparInitCredit(entity.getSeparInitialCredit())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.atError().log("gRPC getWallet failed for account={}: {}", request.getAccountNumber(), e.getMessage());
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getBuyingPower(GetBuyingPowerRequest request, StreamObserver<BuyingPowerResponse> responseObserver) {
        try {
            SettlementDelay settlementDelay = SettlementDelay.valueOf(request.getSettlementDelay());
            BuyingPower bp = walletQueryService.getBuyingPower(request.getAccountNumber(), settlementDelay);

            BuyingPowerResponse response = BuyingPowerResponse.newBuilder()
                    .setBalance(bp.balance())
                    .setCredit(bp.credit())
                    .setSeparCredit(bp.separCredit())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.atError().log("gRPC getBuyingPower failed for account={}: {}", request.getAccountNumber(), e.getMessage());
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}
