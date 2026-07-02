package ir.ebb.wallet.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication(scanBasePackages = {
        "ir.ebb.wallet.app",
        "ir.ebb.wallet.actor",
        "ir.ebb.wallet.service",
        "ir.ebb.wallet.repository",
        "ir.ebb.external.rayan",
        "ir.ebb.userinfo"
})
@EntityScan(basePackages = {
        "ir.ebb.wallet.entity",
        "ir.ebb.external.rayan.wallet.entity",
        "ir.ebb.userinfo.entity"
})
@EnableJpaRepositories(basePackages = {
        "ir.ebb.wallet.repository",
        "ir.ebb.external.rayan.wallet.repository",
        "ir.ebb.userinfo.repository"
})
public class WalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletApplication.class, args);
    }
}
