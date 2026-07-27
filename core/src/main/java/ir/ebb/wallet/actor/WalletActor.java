package ir.ebb.wallet.actor;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.actor.command.CreateWallet;
import ir.ebb.wallet.actor.command.FreezeBalance;
import ir.ebb.wallet.actor.command.WalletCommand;
import ir.ebb.wallet.actor.event.WalletCreated;
import ir.ebb.wallet.actor.event.WalletEvent;
import ir.ebb.wallet.aggregate.WalletAggregate;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;
import org.apache.pekko.pattern.StatusReply;
import org.apache.pekko.persistence.typed.PersistenceId;
import org.apache.pekko.persistence.typed.javadsl.*;

import java.util.function.BiFunction;

public class WalletActor extends EventSourcedBehavior<WalletCommand, WalletEvent, WalletAggregate> {

    public static final EntityTypeKey<WalletCommand> ENTITY_TYPE_KEY =
            EntityTypeKey.create(WalletCommand.class, "WalletActor");

    public static void initSharding(ActorSystem<?> system) {
        ClusterSharding.get(system)
                .init(Entity.of(ENTITY_TYPE_KEY, entityContext ->
                        WalletActor.create(PersistenceId.ofUniqueId(entityContext.getEntityId()))
                ).withRole("wallet"));
    }

    public static Behavior<WalletCommand> create(PersistenceId persistenceId) {
        return Behaviors.setup((ActorContext<WalletCommand> context) -> new WalletActor(persistenceId, context));
    }

    private final ActorContext<WalletCommand> context;

    private WalletActor(PersistenceId persistenceId, ActorContext<WalletCommand> context) {
        super(persistenceId);
        this.context = context;
    }

    @Override
    public WalletAggregate emptyState() {
        return null;
    }

    @Override
    public CommandHandler<WalletCommand, WalletEvent, WalletAggregate> commandHandler() {
        var builder = newCommandHandlerBuilder();

        builder.forNullState()
                .onCommand(CreateWallet.class, (walletAggregate, createWallet) ->
                        Effect().persist(WalletAggregate.create(createWallet))
                                .thenReply(createWallet.replyTo(), param -> StatusReply.ack()))
                .onCommand(WalletCommand.class, (aggregate, command) -> {
                    context.getLog().atError().log("Wallet not found for command {}", command);
                    return Effect().none().thenReply(command.replyTo(), param -> StatusReply.error(new BusinessException("Wallet Not Found", -1)));
                });

        builder.forNonNullState()
                .onCommand(FreezeBalance.class, this::handleFreezeCommand)
                .onAnyCommand(command -> Effect().none()
                        .thenReply(command.replyTo(), param -> StatusReply.error(new BusinessException(ExceptionConstants.INVALID_COMMAND))));
        return builder.build();
    }

    @Override
    public EventHandler<WalletAggregate, WalletEvent> eventHandler() {
        var builder = newEventHandlerBuilder();

        builder.forNullState()
                .onEvent(WalletCreated.class, walletCreated -> WalletAggregate.applyEvent(walletCreated))
                .onAnyEvent(walletEvent -> {
                    context.getLog().atError().log("Event {} is not for null state", walletEvent);
                    return null;
                });

        builder.forNonNullState()
                .onAnyEvent((walletAggregate, walletEvent) -> walletAggregate.applyEvent(walletEvent));

        return builder.build();
    }

    private ReplyEffect<WalletEvent, WalletAggregate> handleFreezeCommand(
            WalletAggregate walletAggregate,
            FreezeBalance freezeBalance
    ) {
        return walletAggregate.validate(freezeBalance)
                .map(balanceFrozen -> Effect().persist(balanceFrozen)
                        .thenReply(freezeBalance.replyTo(), __ -> StatusReply.Ack()))
                .recover(throwable -> Effect().none()
                        .thenReply(freezeBalance.replyTo(), __ -> StatusReply.error(throwable)))
                .get();
    }
}
