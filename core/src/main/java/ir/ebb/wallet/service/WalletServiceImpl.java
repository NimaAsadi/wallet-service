package ir.ebb.wallet.service;

import ir.ebb.base.util.DateUtil;
import ir.ebb.wallet.actor.WalletActor;
import ir.ebb.wallet.actor.command.CreateWallet;
import ir.ebb.wallet.dto.CreateWalletDTO;
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
        var entityId = String.valueOf(requestDTO.dbsAccountNumber()).concat(DateUtil.currentYearWeek());
        return sharding
                .entityRefFor(WalletActor.ENTITY_TYPE_KEY, entityId)
                .askWithStatus(
                        (ActorRef<StatusReply<Done>> replyTo) -> new CreateWallet(requestDTO.dbsAccountNumber(), replyTo),
                        ASK_TIMEOUT
                );
    }
}
