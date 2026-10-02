package ir.ebb.wallet.dto;

import java.util.UUID;

/**
 * Admin credit delta. A zero {@code amount} is the admin remove-credit audit-only no-op.
 */
public record AddCreditDTO(
        Long dbsAccountNumber,
        UUID trackingId,
        Long amount
) {
}
