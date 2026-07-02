package ir.ebb.wallet.app.config;

import com.typesafe.config.ConfigFactory;
import ir.ebb.wallet.actor.WalletRegistryActor;
import ir.ebb.wallet.service.command.WalletCommandService;
import org.apache.pekko.actor.typed.ActorSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PekkaConfig {

    /**
     * Creates the Pekka ActorSystem with WalletRegistryActor as the root behavior.
     * destroyMethod = "terminate" ensures the system shuts down on Spring context close.
     * The blocking dispatcher (wallet-blocking-dispatcher) is loaded from pekka.conf.
     */
    @Bean(destroyMethod = "terminate")
    public ActorSystem<WalletRegistryActor.Command> walletActorSystem(WalletCommandService walletCommandService) {
        return ActorSystem.create(
                WalletRegistryActor.create(walletCommandService),
                "wallet-actor-system",
                ConfigFactory.load("pekka"));
    }
}
