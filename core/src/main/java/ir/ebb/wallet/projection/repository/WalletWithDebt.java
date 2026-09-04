package ir.ebb.wallet.projection.repository;

import ir.ebb.wallet.projection.entity.WalletDebtEntity;
import ir.ebb.wallet.projection.entity.WalletEntity;

/**
 * Joined {@code wallet} + {@code wallet_debt} row pair returned by
 * {@code WalletRepository#selectOneWithDebt} — the two tables are 1:1
 * ({@code wallet_debt.wallet_id} is both PK and FK to {@code wallet.id}).
 *
 * <p>{@code walletDebt} is never null: a wallet with no {@code wallet_debt} row (LEFT JOIN)
 * comes back with a zeroed entity whose {@code walletId} is set to the wallet's id, matching
 * what {@code WalletProjectionAdapter.adapt(WalletDebt, walletId)} would persist.
 */
public record WalletWithDebt(WalletEntity wallet, WalletDebtEntity walletDebt) {
}
