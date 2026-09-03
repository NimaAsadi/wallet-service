package ir.ebb.wallet.projection.entity;

import ir.ebb.common.repository.Column;
import ir.ebb.common.repository.GenerateRepository;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection (read-model) row for the {@code wallet} table, persisted through the generated
 * {@code ir.ebb.wallet.projection.repository.BaseWalletRepository} (R2DBC, via
 * {@code @GenerateRepository}).
 *
 * <p>Generator-compatible flattening of the legacy JPA-shaped {@code ir.ebb.wallet.entity.WalletEntity}:
 * the {@code User} embeddable became {@code userId}/{@code user_id}, the three
 * {@code WalletParameterEmbedded} tiers became six plain columns, and the {@code walletDebtEntity}
 * association is dropped ({@code WalletDebtEntity} maps {@code wallet_debt} separately). {@code id}
 * and {@code version} are declared directly because the generator does not walk superclasses.
 *
 * <p>Generator notes: fields are public because the generated repository (sibling
 * {@code .repository} package) assigns them directly; {@code createdAt}/{@code updatedAt} are
 * excluded from INSERT so the schema's {@code now()} defaults apply — every UPDATE binds
 * {@code updatedAt}, so handlers must populate it before {@code updateOne(...)}.
 */
@Data
@NoArgsConstructor
@GenerateRepository(table = "wallet")
public class WalletEntity {

    public UUID id;

    public long version = 0L;

    public UUID userId;

    public long accountNumber = 0L;

    @Column(name = "t0_balance")
    public long t0Balance = 0L;

    @Column(name = "t0_frozen")
    public long t0Frozen = 0L;

    @Column(name = "t1_balance")
    public long t1Balance = 0L;

    @Column(name = "t1_frozen")
    public long t1Frozen = 0L;

    @Column(name = "t2_balance")
    public long t2Balance = 0L;

    @Column(name = "t2_frozen")
    public long t2Frozen = 0L;

    public long credit = 0L;

    public long initialCredit = 0L;

    public long separCredit = 0L;

    public long separInitialCredit = 0L;

    @Column(insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(insertable = false)
    public LocalDateTime updatedAt;
}
