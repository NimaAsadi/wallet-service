package ir.ebb.common.repository.test.entity;

import ir.ebb.common.repository.Column;
import ir.ebb.common.repository.GenerateRepository;

import java.time.LocalDateTime;

/**
 * Class-shaped test entity for {@link ir.ebb.common.repository.RepositoryProcessor}: exercises the
 * {@code Entity}-suffix stripping ({@code AuditNoteEntity} → {@code BaseAuditNoteRepository}), an
 * explicit {@code @Column(primaryKey = true)} and private fields with hand-written accessors — the
 * generated repository lands in the sibling {@code .repository} package and calls conventional
 * {@code get*}/{@code set*} methods ({@code is*} for primitive booleans), so private fields only
 * compile with accessors present. No Lombok here: it is not on this module's
 * {@code testAnnotationProcessor} path. The {@code archived} field locks in the primitive-boolean
 * {@code is} prefix convention.
 */
@GenerateRepository(table = "audit_notes")
public class AuditNoteEntity {

    @Column(name = "note_id", primaryKey = true)
    private String id;

    @Column(name = "note_text")
    private String noteText;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "archived")
    private boolean archived;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
