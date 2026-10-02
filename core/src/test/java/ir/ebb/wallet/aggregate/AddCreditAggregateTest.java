package ir.ebb.wallet.aggregate;

import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.wallet.actor.command.AddCredit;
import ir.ebb.wallet.actor.command.CreateWallet;
import ir.ebb.wallet.actor.event.CreditAdded;
import ir.ebb.wallet.actor.event.WalletCreated;
import ir.ebb.wallet.actor.event.WalletEvent;
import ir.ebb.wallet.constant.valueobject.Money;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-domain tests for the {@code AddCredit}/{@code CreditAdded} delta — no DB, no actor system.
 * Mirrors the legacy addCredit semantics: initialCredit and credit both increase by the amount;
 * remove-credit (amount 0) is an audit-only no-op. {@code validate} returns a failed {@link Try}
 * (it does not throw), so rejections are asserted through the Try.
 */
class AddCreditAggregateTest {

    private static final long ACCOUNT = 123L;

    private WalletAggregate created() {
        return WalletAggregate.applyEvent(WalletAggregate.create(new CreateWallet(ACCOUNT, null)));
    }

    @Test
    void validate_returnsCreditAddedStampedWithAccountNumber() {
        WalletAggregate aggregate = created();
        Try<WalletEvent> result =
                aggregate.validate(new AddCredit(UUID.randomUUID(), new Money(250L), null));

        assertThat(result.isSuccess()).isTrue();
        CreditAdded event = (CreditAdded) result.get();
        assertThat(event.dbsAccountNumber()).isEqualTo(ACCOUNT);
        assertThat(event.value()).isEqualTo(new Money(250L));
    }

    @Test
    void applyEvent_increasesCreditAndInitialCredit() {
        WalletAggregate aggregate = created();
        aggregate.applyEvent(new CreditAdded(UUID.randomUUID(), new Money(250L), ACCOUNT));

        assertThat(aggregate.getCredit()).isEqualTo(250L);
        assertThat(aggregate.getInitialCredit()).isEqualTo(250L);

        aggregate.applyEvent(new CreditAdded(UUID.randomUUID(), new Money(50L), ACCOUNT));
        assertThat(aggregate.getCredit()).isEqualTo(300L);
        assertThat(aggregate.getInitialCredit()).isEqualTo(300L);
    }

    @Test
    void duplicateTrackingId_failsTheTry() {
        WalletAggregate aggregate = created();
        UUID trackingId = UUID.randomUUID();
        aggregate.applyEvent(new CreditAdded(trackingId, new Money(100L), ACCOUNT));

        Try<WalletEvent> result = aggregate.validate(new AddCredit(trackingId, new Money(100L), null));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate tracking id");
    }

    @Test
    void zeroAmount_isAuditOnlyNoOp() {
        WalletAggregate aggregate = created();
        aggregate.applyEvent(new CreditAdded(UUID.randomUUID(), new Money(0L), ACCOUNT));

        assertThat(aggregate.getCredit()).isZero();
        assertThat(aggregate.getInitialCredit()).isZero();
        assertThat(aggregate.getTrackingIds()).hasSize(1); // still an idempotency marker
    }
}
