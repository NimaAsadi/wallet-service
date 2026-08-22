package ir.ebb.wallet.service;

import ir.ebb.base.util.DateUtil;
import ir.ebb.wallet.actor.WalletActor;
import ir.ebb.wallet.actor.command.*;
import ir.ebb.wallet.constant.valueobject.Money;
import ir.ebb.wallet.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.pattern.StatusReply;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(10);

    private final ClusterSharding sharding;

    @Override
    public CompletionStage<Done> createWallet(CreateWalletDTO requestDTO) {
        return sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId(requestDTO.dbsAccountNumber()))
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new CreateWallet(requestDTO.dbsAccountNumber(), replyTo),
                        ASK_TIMEOUT
                );
    }

    @Override
    public CompletionStage<Done> deposit(DepositDTO requestDTO) {
        return sharding
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
                );
    }

    @Override
    public CompletionStage<Done> withdraw(WithdrawDTO requestDTO) {
        return sharding
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
                );
    }

    @Override
    public CompletionStage<Done> freeze(FreezeDTO requestDTO) {
        return sharding
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
                );
    }

    @Override
    public CompletionStage<Done> unfreeze(UnfreezeDTO requestDTO) {
        return sharding
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
                );
    }

    @Override
    public CompletionStage<Done> spend(SpendDTO requestDTO) {
        return sharding
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
                );
    }

    private static String entityId(Long dbsAccountNumber) {
        return String.valueOf(dbsAccountNumber).concat(DateUtil.currentYearWeek());
    }
}
