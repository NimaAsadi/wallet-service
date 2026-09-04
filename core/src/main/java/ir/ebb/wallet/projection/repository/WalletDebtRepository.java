package ir.ebb.wallet.projection.repository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WalletDebtRepository extends BaseWalletDebtRepository {

    /** Explicit for Dagger — the inherited default ctor is invisible to annotation processing. */
    @Inject
    public WalletDebtRepository() {
    }
}
