package ir.ebb.wallet.actor;

import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.actor.command.*;
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
import org.apache.pekko.persistence.typed.javadsl.CommandHandler;
import org.apache.pekko.persistence.typed.javadsl.EventHandler;
import org.apache.pekko.persistence.typed.javadsl.EventSourcedBehavior;
import org.apache.pekko.persistence.typed.javadsl.ReplyEffect;

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
                    return Effect().none().thenReply(command.replyTo(), param -> StatusReply.error(
                            new BusinessException(ExceptionConstants.WALLET_NOT_EXIST.getMessage(),
                                    ExceptionConstants.WALLET_NOT_EXIST.getCode())));
                });

        builder.forNonNullState()
                .onCommand(Freeze.class, this::handleCommand)
                .onCommand(Spend.class, this::handleCommand)
                .onCommand(Deposit.class, this::handleCommand)
                .onCommand(Unfreeze.class,this::handleCommand)
                .onCommand(Withdraw.class,this::handleCommand)
                .onCommand(AddCredit.class,this::handleCommand)
                // Read-only, strongly-consistent reads — served from the in-memory aggregate,
                // nothing persisted, no idempotency/tracking check.
                .onCommand(GetWallet.class, (aggregate, getWallet) ->
                        Effect().none().thenReply(getWallet.replyTo(), __ -> StatusReply.success(aggregate.toSnapshot())))
                .onCommand(GetBuyingPower.class, (aggregate, getBuyingPower) ->
                        Effect().none().thenReply(getBuyingPower.replyTo(), __ ->
                                StatusReply.success(aggregate.buyingPower(getBuyingPower.settlementDelay()))))
                .onAnyCommand(command -> Effect().none()
                        .thenReply(command.replyTo(), param -> StatusReply.error(new BusinessException(ExceptionConstants.INVALID_COMMAND))));
        return builder.build();
    }

    @Override
    public EventHandler<WalletAggregate, WalletEvent> eventHandler() {
        var builder = newEventHandlerBuilder();

        builder.forNullState()
                // Deep copy, never alias: see WalletAggregate.copyOf — the event embeds the state
                // and the live state mutates on every applied event.
                .onEvent(WalletCreated.class, walletCreated -> WalletAggregate.copyOf(walletCreated.wallet()))
                .onAnyEvent(walletEvent -> {
                    context.getLog().atError().log("Event {} is not for null state", walletEvent);
                    return null;
                });

        builder.forNonNullState()
                .onAnyEvent((walletAggregate, walletEvent) -> walletAggregate.applyEvent(walletEvent));

        return builder.build();
    }

    private ReplyEffect<WalletEvent, WalletAggregate> handleCommand(
            WalletAggregate walletAggregate,
            WalletCommand walletCommand
    ) {
        return walletAggregate.validate(walletCommand)
                .map(walletEvent -> Effect().persist(walletEvent)
                        .thenReply(walletCommand.replyTo(), __ -> StatusReply.Ack()))
                .recover(throwable -> Effect().none()
                        .thenReply(walletCommand.replyTo(), __ -> StatusReply.error(throwable)))
                .get();
    }
}
