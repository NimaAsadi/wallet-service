package ir.ebb.wallet.projection.entity;

import ir.ebb.common.repository.Column;
import ir.ebb.common.repository.GenerateRepository;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection (read-model) row for the {@code wallet_debt} table, persisted through the generated
 * {@code ir.ebb.wallet.projection.repository.BaseWalletDebtRepository} (R2DBC, via
 * {@code @GenerateRepository}).
 *
 * <p>The table's primary key is {@code wallet_id} (the owning wallet's id, FK to
 * {@code wallet.id}) — not {@code id} — hence the explicit {@code @Column(primaryKey = true)}.
 * Maps all seven debt counters persisted in the schema: the three tier-to-tier counters
 * (primitive, NOT NULL) and the four credit/separ-credit counters (boxed, nullable).
 *
 * <p>Generator notes: fields are public because the generated repository (sibling
 * {@code .repository} package) assigns them directly; {@code createdAt}/{@code updatedAt} are
 * excluded from INSERT so the schema's {@code now()} defaults apply — every UPDATE binds
 * {@code updatedAt}, so handlers must populate it before {@code updateOne(...)}.
 */
@Data
@NoArgsConstructor
@GenerateRepository(table = "wallet_debt")
public class WalletDebtEntity {

    @Column(name = "wallet_id", primaryKey = true)
    public UUID walletId;

    @Column(name = "t2_to_t0_debt")
    public long t2ToT0Debt = 0L;

    @Column(name = "t2_to_t1_debt")
    public long t2ToT1Debt = 0L;

    @Column(name = "t1_to_t0_debt")
    public long t1ToT0Debt = 0L;

    @Column(name = "t2_to_credit_debt")
    public Long t2ToCreditDebt = 0L;

    @Column(name = "t1_to_credit_debt")
    public Long t1ToCreditDebt = 0L;

    @Column(name = "t2_to_separ_credit_debt")
    public Long t2ToSeparCreditDebt = 0L;

    @Column(name = "t1_to_separ_credit_debt")
    public Long t1ToSeparCreditDebt = 0L;

    @Column(insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(insertable = false)
    public LocalDateTime updatedAt;
}
