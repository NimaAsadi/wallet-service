package ir.ebb.wallet.actor.event;

import ir.ebb.wallet.constant.valueobject.Money;

import java.util.UUID;

/**
 * Credit delta: bumps {@code initialCredit} and {@code credit} by the same amount (no settlement
 * tier is touched, so there is no {@code settlementDelay}; the read-model leg hardcodes
 * {@code WalletTransactionType.ADMIN_CREDIT}). Carries {@code dbsAccountNumber} — the
 * projection's join key.
 */
public record CreditAdded(
        UUID trackingId,
        Money value,
        Long dbsAccountNumber
) implements WalletEvent {
}
