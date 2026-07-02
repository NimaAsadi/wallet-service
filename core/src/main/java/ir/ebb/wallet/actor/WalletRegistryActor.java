package ir.ebb.wallet.actor;

import ir.ebb.wallet.actor.message.WalletCommand;
import ir.ebb.wallet.service.command.WalletCommandService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.DispatcherSelector;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton root actor that routes WalletCommands to the correct DailyWalletActor.
 *
 * Routing key: "{accountNumber}_{date}" — one actor per user per calendar day.
 * The registry runs on the default fork-join dispatcher (no blocking I/O here).
 * Each DailyWalletActor is spawned on wallet-blocking-dispatcher so its JDBC
 * calls don't starve the scheduler.
 *
 * When a DailyWalletActor passivates it sends Deregistering to the registry
 * (which immediately removes the key from the map) before stopping. watchWith
 * also fires ChildTerminated as a safety net for unexpected termination.
 */
@Slf4j
public class WalletRegistryActor extends AbstractBehavior<WalletRegistryActor.Command> {

    static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");

    // ── command protocol ──────────────────────────────────────────────────────

    public sealed interface Command {
        record Route(long accountNumber, WalletCommand command) implements Command {}

        /** Sent by a DailyWalletActor just before it stops (passivation). */
        record Deregistering(String key) implements Command {}

        /** Safety-net: sent by watchWith after any child terminates unexpectedly. */
        record ChildTerminated(String key) implements Command {}
    }

    // ── state ─────────────────────────────────────────────────────────────────

    private final Map<String, ActorRef<WalletCommand>> actors = new HashMap<>();
    private final WalletCommandService walletCommandService;

    // ── factory ───────────────────────────────────────────────────────────────

    public static Behavior<Command> create(WalletCommandService walletCommandService) {
        return Behaviors.setup(ctx -> new WalletRegistryActor(ctx, walletCommandService));
    }

    private WalletRegistryActor(ActorContext<Command> ctx, WalletCommandService walletCommandService) {
        super(ctx);
        this.walletCommandService = walletCommandService;
    }

    // ── dispatch ──────────────────────────────────────────────────────────────

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Command.Route.class, this::onRoute)
                .onMessage(Command.Deregistering.class, msg -> {
                    actors.remove(msg.key());
                    log.debug("DailyWalletActor deregistered: {}", msg.key());
                    return this;
                })
                .onMessage(Command.ChildTerminated.class, msg -> {
                    actors.remove(msg.key());
                    log.debug("DailyWalletActor terminated: {}", msg.key());
                    return this;
                })
                .build();
    }

    private Behavior<Command> onRoute(Command.Route msg) {
        String key = msg.accountNumber() + "_" + LocalDate.now(TEHRAN);
        ActorRef<WalletCommand> actorRef = actors.computeIfAbsent(key, k -> {
            log.debug("Spawning DailyWalletActor for key {}", k);
            ActorRef<WalletCommand> ref = getContext().spawn(
                    DailyWalletActor.create(walletCommandService, getContext().getSelf(), k),
                    k,
                    DispatcherSelector.fromConfig("wallet-blocking-dispatcher"));
            getContext().watchWith(ref, new Command.ChildTerminated(k));
            return ref;
        });
        actorRef.tell(msg.command());
        return this;
    }
}
