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
 * <p>Generator notes: fields are private — the generated repository (sibling {@code .repository}
 * package) reads and writes them through the Lombok {@code @Data} accessors;
 * {@code createdAt}/{@code updatedAt} are
 * excluded from INSERT so the schema's {@code now()} defaults apply — every UPDATE binds
 * {@code updatedAt}, so handlers must populate it before {@code updateOne(...)}.
 */
@Data
@NoArgsConstructor
@GenerateRepository(table = "wallet")
public class WalletEntity {

    private UUID id;

    private long version = 0L;

    private long accountNumber = 0L;

    @Column(name = "t0_balance")
    private long t0Balance = 0L;

    @Column(name = "t0_frozen")
    private long t0Frozen = 0L;

    @Column(name = "t1_balance")
    private long t1Balance = 0L;

    @Column(name = "t1_frozen")
    private long t1Frozen = 0L;

    @Column(name = "t2_balance")
    private long t2Balance = 0L;

    @Column(name = "t2_frozen")
    private long t2Frozen = 0L;

    private long credit = 0L;

    private long initialCredit = 0L;

    private long separCredit = 0L;

    private long separInitialCredit = 0L;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(insertable = false)
    private LocalDateTime updatedAt;

}
