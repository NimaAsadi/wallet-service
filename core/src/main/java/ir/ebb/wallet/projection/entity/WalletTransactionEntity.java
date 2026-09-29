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
 * <p>Generator notes: fields are private — the generated repository (sibling {@code .repository}
 * package) reads and writes them through the Lombok {@code @Data} accessors; the enum columns are nullable in the schema
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

    private UUID id;

    private long version = 0L;

    private long accountNumber = 0L;

    private UUID walletId;

    private WalletOperationType walletOperationType;

    private WalletTransactionType walletTransactionType;

    private WalletParameterType walletParameterType;

    private long amount = 0L;

    private UUID trackingId;

    private Long frozenBefore;

    private Long frozenAfter;

    private Long balanceBefore;

    private Long balanceAfter;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
