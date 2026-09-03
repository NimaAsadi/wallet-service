package ir.ebb.common.repository.test.entity;

import ir.ebb.common.repository.Column;
import ir.ebb.common.repository.GenerateRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Record-shaped test entity for {@link ir.ebb.common.repository.RepositoryProcessor}: exercises the
 * default {@code id}-field PK convention, a {@code @Column(name = ...)} override, camelCase→snake_case
 * column derivation, an enum component (bound via {@code name()}, read via {@code valueOf}) and an
 * {@code insertable = false, updatable = false} column.
 */
@GenerateRepository(table = "ledger_entries")
public record LedgerEntryProjection(
        UUID id,
        @Column(name = "account_num") String accountNumber,
        UUID walletId,
        EntryKind kind,
        Long amount,
        @Column(insertable = false, updatable = false) LocalDateTime createdAt) {

    public enum EntryKind { DEBIT, CREDIT }
}
