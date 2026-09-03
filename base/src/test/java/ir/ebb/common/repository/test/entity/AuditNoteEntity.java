package ir.ebb.common.repository.test.entity;

import ir.ebb.common.repository.Column;
import ir.ebb.common.repository.GenerateRepository;

import java.time.LocalDateTime;

/**
 * Class-shaped test entity for {@link ir.ebb.common.repository.RepositoryProcessor}: exercises the
 * {@code Entity}-suffix stripping ({@code AuditNoteEntity} → {@code BaseAuditNoteRepository}), an
 * explicit {@code @Column(primaryKey = true)} and public fields (required because the generated
 * repository lands in the sibling {@code .repository} package and accesses fields directly).
 */
@GenerateRepository(table = "audit_notes")
public class AuditNoteEntity {

    @Column(name = "note_id", primaryKey = true)
    public String id;

    @Column(name = "note_text")
    public String noteText;

    @Column(insertable = false, updatable = false)
    public LocalDateTime createdAt;
}
