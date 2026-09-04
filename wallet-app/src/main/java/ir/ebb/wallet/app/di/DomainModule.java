package ir.ebb.wallet.app.di;

import dagger.Module;
import dagger.Binds;
import ir.ebb.wallet.service.credit.query.CreditHistoryQueryService;
import ir.ebb.wallet.service.credit.query.CreditHistoryQueryServiceImpl;
import ir.ebb.wallet.service.query.WalletQueryService;
import ir.ebb.wallet.service.query.WalletQueryServiceImpl;
import ir.ebb.wallet.service.transaction.query.WalletTransactionQueryService;
import ir.ebb.wallet.service.transaction.query.WalletTransactionQueryServiceImpl;
import ir.ebb.wallet.service.turnover.command.TurnoverCommandService;
import ir.ebb.wallet.service.turnover.command.TurnoverCommandServiceImpl;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryService;
import ir.ebb.wallet.service.turnover.query.TurnoverQueryServiceImpl;

/** Read-side + turnover domain services (core module). */
@Module
public abstract class DomainModule {

    @Binds
    abstract WalletQueryService walletQueryService(WalletQueryServiceImpl impl);

    @Binds
    abstract TurnoverQueryService turnoverQueryService(TurnoverQueryServiceImpl impl);

    @Binds
    abstract TurnoverCommandService turnoverCommandService(TurnoverCommandServiceImpl impl);

    @Binds
    abstract CreditHistoryQueryService creditHistoryQueryService(CreditHistoryQueryServiceImpl impl);

    @Binds
    abstract WalletTransactionQueryService walletTransactionQueryService(WalletTransactionQueryServiceImpl impl);
}
