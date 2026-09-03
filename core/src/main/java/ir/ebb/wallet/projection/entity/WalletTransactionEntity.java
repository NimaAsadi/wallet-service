package ir.ebb.wallet.projection.entity;

import ir.ebb.common.repository.Column;
import ir.ebb.common.repository.GenerateRepository;
import ir.ebb.wallet.constant.enumeration.WalletOperationType;
import ir.ebb.wallet.constant.enumeration.WalletParameterType;
import ir.ebb.wallet.constant.enumeration.WalletTransactionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection (read-model) row for the {@code wallet_transaction} table, persisted through the
 * generated {@code ir.ebb.wallet.projection.repository.BaseWalletTransactionRepository} (R2DBC,
 * via {@code @GenerateRepository}).
 *
 * <p>Generator-compatible flattening of the legacy {@code ir.ebb.wallet.entity.WalletTransactionEntity}:
 * the {@code User} embeddable became {@code userId}/{@code user_id}; everything else maps 1:1.
 * Primitives for NOT NULL columns, boxed {@code Long} for the nullable before/after audit columns.
 *
 * <p>Generator notes: fields are public because the generated repository (sibling
 * {@code .repository} package) assigns them directly; the enum columns are nullable in the schema
 * but the generated {@code mapRow} does unguarded {@code valueOf} — NULL values would NPE on read
 * (this projection always writes all three; override {@code mapRow} in a hand-written subclass if
 * legacy NULL rows appear); {@code createdAt}/{@code updatedAt} are excluded from INSERT so the
 * schema's {@code now()} defaults apply — every UPDATE binds {@code updatedAt}, so handlers must
 * populate it before {@code updateOne(...)}.
 */
@Data
@NoArgsConstructor
@GenerateRepository(table = "wallet_transaction")
public class WalletTransactionEntity {

    public UUID id;

    public long version = 0L;

    public UUID userId;

    public long accountNumber = 0L;

    public UUID walletId;

    public WalletOperationType walletOperationType;

    public WalletTransactionType walletTransactionType;

    public WalletParameterType walletParameterType;

    public long amount = 0L;

    public UUID trackingId;

    public Long frozenBefore;

    public Long frozenAfter;

    public Long balanceBefore;

    public Long balanceAfter;

    @Column(insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(insertable = false)
    public LocalDateTime updatedAt;
}
