package ir.ebb.wallet.app.di;

import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import ir.ebb.external.rayan.wallet.service.command.RayanWalletHistoryCommandService;
import ir.ebb.external.rayan.wallet.service.query.RayanWalletQueryService;
import ir.ebb.wallet.app.admin.service.RayanWalletJobService;
import ir.ebb.wallet.app.admin.service.job.TurnoverNotifyWebService;
import ir.ebb.wallet.app.web.CronScheduler;
import ir.ebb.wallet.app.web.WalletJobs;

import javax.inject.Singleton;

/** The four scheduled jobs and their cron expressions. */
@Module
public abstract class JobsModule {

    /** Provided here (not {@code @Inject}) because of the four cron-expression config params. */
    @Provides
    @Singleton
    public static WalletJobs walletJobs(
            CronScheduler cronScheduler,
            RayanWalletQueryService rayanWalletQueryService,
            RayanWalletJobService rayanWalletJobService,
            RayanWalletHistoryCommandService rayanWalletHistoryCommandService,
            TurnoverNotifyWebService turnoverNotifyWebService,
            Config config) {
        return new WalletJobs(cronScheduler, rayanWalletQueryService, rayanWalletJobService,
                rayanWalletHistoryCommandService, turnoverNotifyWebService,
                config.getString("wallet.schedule.rayan"),
                config.getString("wallet.schedule.rayan-backup"),
                config.getString("wallet.schedule.rayan-remove"),
                config.getString("wallet.schedule.turnover-notify"));
    }
}
