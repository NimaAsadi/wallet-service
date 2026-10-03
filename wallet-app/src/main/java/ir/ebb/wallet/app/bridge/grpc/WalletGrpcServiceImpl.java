package ir.ebb.wallet.app.bridge.grpc;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.grpc.BuyingPowerResponse;
import ir.ebb.wallet.grpc.GetBuyingPowerRequest;
import ir.ebb.wallet.grpc.GetWalletRequest;
import ir.ebb.wallet.grpc.WalletResponse;
import ir.ebb.wallet.grpc.WalletServiceGrpc;
import ir.ebb.wallet.service.WalletErrors;
import ir.ebb.wallet.service.WalletService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Bridge reads served by the sharded entity ({@code WalletService} → {@code GetWallet} /
 * {@code GetBuyingPower}): strongly consistent, never behind the projection. The asks are async,
 * so each RPC composes the stage and completes the observer from {@code whenComplete};
 * {@link WalletErrors#rootCause(Throwable)} unwraps the {@code CompletionException} layers so
 * {@link #toStatus} sees the raw {@code BusinessException}.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WalletGrpcServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletService walletService;

    @Override
    public void getWallet(GetWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        walletService.getWallet(request.getAccountNumber())
                .whenComplete((snapshot, error) -> {
                    if (error != null) {
                        fail(request.getAccountNumber(), "getWallet", responseObserver, error);
                        return;
                    }
                    long t0 = snapshot.t0().getBalance();
                    long t1 = snapshot.t1().getBalance();
                    long t2 = snapshot.t2().getBalance();
                    long frozen = snapshot.t0().getFrozen() + snapshot.t1().getFrozen() + snapshot.t2().getFrozen();

                    WalletResponse response = WalletResponse.newBuilder()
                            .setBalance(t0 + t1 + t2)
                            .setT0(t0)
                            .setT1(t1 + t0)
                            .setT2(t2 + t1 + t0)
                            .setTotalFrozen(frozen)
                            .setCredit(snapshot.credit())
                            .setInitCredit(snapshot.initialCredit())
                            .setSeparCredit(snapshot.separCredit())
                            .setSeparInitCredit(snapshot.separInitialCredit())
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                });
    }

    @Override
    public void getBuyingPower(GetBuyingPowerRequest request, StreamObserver<BuyingPowerResponse> responseObserver) {
        parseAndFetch(request)
                .whenComplete((buyingPower, error) -> {
                    if (error != null) {
                        fail(request.getAccountNumber(), "getBuyingPower", responseObserver, error);
                        return;
                    }
                    BuyingPowerResponse response = BuyingPowerResponse.newBuilder()
                            .setBalance(buyingPower.balance())
                            .setCredit(buyingPower.credit())
                            .setSeparCredit(buyingPower.separCredit())
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                });
    }

    /** Inside the stage so a bad {@code settlementDelay} string maps to INTERNAL, not an uncaught throw. */
    private CompletionStage<BuyingPower> parseAndFetch(GetBuyingPowerRequest request) {
        return CompletableFuture.supplyAsync(() -> SettlementDelay.valueOf(request.getSettlementDelay()))
                .thenCompose(settlementDelay ->
                        walletService.getBuyingPower(request.getAccountNumber(), settlementDelay));
    }

    private void fail(long accountNumber, String op, StreamObserver<?> observer, Throwable error) {
        Throwable cause = WalletErrors.rootCause(error);
        log.atError().log("gRPC {} failed for account={}: {}", op, accountNumber, cause.getMessage());
        observer.onError(toStatus(cause)
                .withDescription(cause.getMessage())
                .asRuntimeException());
    }

    /** WALLET_NOT_EXIST (missing wallet) → NOT_FOUND; anything else (e.g. actor unavailable) → INTERNAL. */
    private static io.grpc.Status toStatus(Throwable e) {
        if (e instanceof BusinessException be && be.getCode() != null
                && Objects.equals(be.getCode(), ExceptionConstants.WALLET_NOT_EXIST.getCode())) {
            return io.grpc.Status.NOT_FOUND;
        }
        return io.grpc.Status.INTERNAL;
    }
}
