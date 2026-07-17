package ir.ebb.wallet.aggregate;

import ir.ebb.common.constant.enumeration.SettlementDelay;
import ir.ebb.common.exception.handler.ApplicationException;
import ir.ebb.common.model.user.User;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import ir.ebb.wallet.constant.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure-domain tests for the {@link Wallet} aggregate — no Spring, no DB, no actor system.
 * Guards the money-movement cascade that the event-sourced {@code WalletEntity} reuses
 * verbatim ({@code state.toAggregate()} → mutate → {@code WalletState.fromAggregate()}).
 */
class WalletAggregateTest {

    private static final SettlementDelay T0 = SettlementDelay.T_PLUS_0;
    private static final WalletTransactionType TYPE = WalletTransactionType.BANK_GATEWAY;

    private final UUID tracking = UUID.randomUUID();
    private final Wallet wallet = new Wallet(User.of(UUID.randomUUID(), 123L));

    @Test
    void depositT0_increasesBalanceAndBuyingPower() throws Exception {
        wallet.deposit(tracking, new Money(100L), T0, TYPE);

        assertThat(wallet.getT0().getBalance()).isEqualTo(100L);
        assertThat(wallet.getT0().getFrozen()).isZero();
        assertThat(wallet.buyingPower(T0).balance()).isEqualTo(100L);
        assertThat(wallet.getWalletTransactions()).isNotEmpty();
    }

    @Test
    void withdraw_decreasesBalance() throws Exception {
        wallet.deposit(tracking, new Money(100L), T0, TYPE);
        wallet.withdraw(tracking, new Money(40L), T0, TYPE);

        assertThat(wallet.getT0().getBalance()).isEqualTo(60L);
    }

    @Test
    void freeze_movesBalanceToFrozen() throws Exception {
        wallet.deposit(tracking, new Money(100L), T0, TYPE);
        wallet.freeze(tracking, new Money(100L), T0, TYPE, false);

        assertThat(wallet.getT0().getBalance()).isZero();
        assertThat(wallet.getT0().getFrozen()).isEqualTo(100L);
    }

    @Test
    void spend_consumesFrozen() throws Exception {
        wallet.deposit(tracking, new Money(100L), T0, TYPE);
        wallet.freeze(tracking, new Money(100L), T0, TYPE, false);
        wallet.spend(tracking, new Money(30L), T0, TYPE);

        assertThat(wallet.getT0().getFrozen()).isEqualTo(70L);
    }

    @Test
    void freeze_exceedingBuyingPower_throws() throws Exception {
        assertThatThrownBy(() -> wallet.freeze(tracking, new Money(1L), T0, TYPE, false))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    void spend_exceedingFrozen_throws() throws Exception {
        wallet.deposit(tracking, new Money(100L), T0, TYPE);
        wallet.freeze(tracking, new Money(10L), T0, TYPE, false);

        assertThatThrownBy(() -> wallet.spend(tracking, new Money(50L), T0, TYPE))
                .isInstanceOf(ApplicationException.class);
    }
}
