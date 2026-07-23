package ir.ebb.wallet.wallet;

import ir.ebb.wallet.aggregate.WalletTransaction;

import java.util.List;

/**
 * Persisted domain events of the {@link WalletActor}. Serialized to the event journal
 * as JSON via the {@link WalletSerializable} marker.
 *
 * <p>{@link WalletMutated} carries the audit {@code legs} produced by the aggregate
 * (each leg is a {@link WalletTransaction} with operation/parameter type, amount, and
 * before/after balances — exactly the existing audit-row shape) plus the full resulting
 * {@link WalletState}. The event handler sets state from {@code resultingState}
 * (absolute values → idempotent); the {@code legs} feed the {@code wallet_transaction}
 * read-model projection.
 */
public sealed interface WalletEvent extends WalletSerializable {

    /** First event: establishes the wallet's identity + empty balances. */
    record WalletCreated(WalletState initialState) implements WalletEvent {}

    /** A money-movement / credit / reconciliation result. {@code legs} may be empty (e.g. a no-op or Rayan reconcile). */
    record WalletMutated(List<WalletTransaction> legs, WalletState resultingState) implements WalletEvent {}

    /** Migration seed: sets the initial state from a legacy {@code wallet} row. */
    record WalletSeeded(WalletState state) implements WalletEvent {}
}
