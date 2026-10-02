package ir.ebb.wallet.service;

import ir.ebb.base.util.DateUtil;
import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.wallet.actor.WalletActor;
import ir.ebb.wallet.actor.WalletSnapshot;
import ir.ebb.wallet.actor.command.*;
import ir.ebb.wallet.constant.valueobject.BuyingPower;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.pattern.StatusReply;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WalletServiceImpl implements WalletService {

    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(10);

    private final ClusterSharding sharding;

    @Override
    public CompletionStage<WalletSnapshot> getWallet(Long dbsAccountNumber) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(dbsAccountNumber))
                .askWithStatus(
                        (ActorRef<StatusReply<WalletSnapshot>> replyTo) -> new GetWallet(replyTo),
                        ASK_TIMEOUT
                ));
    }

    @Override
    public CompletionStage<BuyingPower> getBuyingPower(Long dbsAccountNumber, SettlementDelay settlementDelay) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(dbsAccountNumber))
                .askWithStatus(
                        (ActorRef<StatusReply<BuyingPower>> replyTo) -> new GetBuyingPower(settlementDelay, replyTo),
                        ASK_TIMEOUT
                ));
    }

    @Override
    public CompletionStage<Done> createWallet(CreateWalletDTO requestDTO) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(requestDTO.dbsAccountNumber()))
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new CreateWallet(requestDTO.dbsAccountNumber(), replyTo),
                        ASK_TIMEOUT
                ));
    }

    @Override
    public CompletionStage<Done> deposit(DepositDTO requestDTO) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(requestDTO.dbsAccountNumber()))
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new Deposit(
                                requestDTO.trackingId(),
                                new Money(requestDTO.amount()),
                                requestDTO.settlementDelay(),
                                requestDTO.walletTransactionType(),
                                replyTo
                        ),
                        ASK_TIMEOUT
                ));
    }

    @Override
    public CompletionStage<Done> withdraw(WithdrawDTO requestDTO) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(requestDTO.dbsAccountNumber()))
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new Withdraw(
                                requestDTO.trackingId(),
                                new Money(requestDTO.amount()),
                                requestDTO.settlementDelay(),
                                requestDTO.walletTransactionType(),
                                replyTo
                        ),
                        ASK_TIMEOUT
                ));
    }

    @Override
    public CompletionStage<Done> freeze(FreezeDTO requestDTO) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(requestDTO.dbsAccountNumber()))
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new Freeze(
                                requestDTO.trackingId(),
                                new Money(requestDTO.amount()),
                                requestDTO.settlementDelay(),
                                requestDTO.walletTransactionType(),
                                requestDTO.canSpendSeparCredit(),
                                replyTo
                        ),
                        ASK_TIMEOUT
                ));
    }

    @Override
    public CompletionStage<Done> unfreeze(UnfreezeDTO requestDTO) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(requestDTO.dbsAccountNumber()))
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new Unfreeze(
                                requestDTO.trackingId(),
                                new Money(requestDTO.amount()),
                                requestDTO.settlementDelay(),
                                requestDTO.walletTransactionType(),
                                requestDTO.canSpendSeparCredit(),
                                replyTo
                        ),
                        ASK_TIMEOUT
                ));
    }

    @Override
    public CompletionStage<Done> spend(SpendDTO requestDTO) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(requestDTO.dbsAccountNumber()))
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new Spend(
                                requestDTO.trackingId(),
                                new Money(requestDTO.amount()),
                                requestDTO.settlementDelay(),
                                requestDTO.walletTransactionType(),
                                requestDTO.canSpendSeparCredit(),
                                replyTo
                        ),
                        ASK_TIMEOUT
                ));
    }

    @Override
    public CompletionStage<Done> addCredit(AddCreditDTO requestDTO) {
        return unwrap(sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(requestDTO.dbsAccountNumber()))
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new AddCredit(
                                requestDTO.trackingId(),
                                new Money(requestDTO.amount()),
                                replyTo
                        ),
                        ASK_TIMEOUT
                ));
    }

    /**
     * Re-throws the root cause so callers see the raw {@code BusinessException} instead of the
     * {@code CompletionException} wrapper a failed {@code askWithStatus} stage carries.
     */
    private static <T> CompletionStage<T> unwrap(CompletionStage<T> stage) {
        return WalletErrors.unwrap(stage);
    }

    private static String entityId(Long dbsAccountNumber) {
        return String.valueOf(dbsAccountNumber).concat(DateUtil.currentYearWeek());
    }
}
