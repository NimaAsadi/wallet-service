package ir.ebb.wallet.infrastructure.migration;

import ir.ebb.wallet.aggregate.Wallet;
import ir.ebb.wallet.entity.WalletEntity;
import ir.ebb.wallet.repository.WalletRepository;
import ir.ebb.wallet.wallet.WalletFacade;
import ir.ebb.wallet.wallet.WalletState;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * One-off migration that imports existing mutable {@code wallet} rows into the event journal.
 * For each wallet row it rebuilds the {@link WalletState} and sends a fire-and-forget
 * {@code SeedFromLegacy} tell to the sharded wallet entity; the entity persists exactly one
 * {@code WalletSeeded} event (idempotent — a no-op if the entity was already created). The
 * read-model projection upserts the same state, so existing read rows stay consistent.
 *
 * <p>Cutover runbook (hard cutover, idempotent, no dual-write):
 * <ol>
 *   <li>stop old writes / drain the previous deployment;</li>
 *   <li>deploy this service (cluster + sharding + persistence + projections up, writes off);</li>
 *   <li>run with {@code --seed-from-legacy} once — existing balances become {@code WalletSeeded}
 *       events; keep the process running until the projection settles (offset store advances,
 *       read tables match);</li>
 *   <li>enable new HTTP writes; cut traffic. Re-running {@code --seed-from-legacy} is a no-op.</li>
 * </ol>
 */
@Slf4j
public final class LegacySeeder {

    private LegacySeeder() {}

    public static void run(WalletRepository walletRepository, WalletFacade walletFacade) {
        List<WalletEntity> wallets = walletRepository.findAll();
        log.info("--seed-from-legacy: seeding {} legacy wallets into the event journal", wallets.size());
        int sent = 0;
        for (WalletEntity entity : wallets) {
            try {
                Wallet wallet = entity.adaptToDomain();
                WalletState state = WalletState.fromAggregate(wallet, List.of());
                walletFacade.seedFromLegacy(state);
                sent++;
            } catch (Exception e) {
                log.warn("--seed-from-legacy: failed to seed wallet {}: {}", entity.getId(), e.getMessage());
            }
        }
        log.info("--seed-from-legacy: queued {} of {} SeedFromLegacy tells (fire-and-forget)", sent, wallets.size());
    }
}
