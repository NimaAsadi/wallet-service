package ir.ebb.common.repository.test;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import ir.ebb.common.repository.BaseRepository;
import ir.ebb.common.repository.test.entity.AuditNoteEntity;
import ir.ebb.common.repository.test.entity.LedgerEntryProjection;
import ir.ebb.common.repository.test.repository.BaseAuditNoteRepository;
import ir.ebb.common.repository.test.repository.BaseLedgerEntryProjectionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof that {@link ir.ebb.common.repository.RepositoryProcessor} ran during test
 * compilation: the imports above only resolve because the processor generated
 * {@code BaseLedgerEntryProjectionRepository} / {@code BaseAuditNoteRepository} into the test
 * source set's generated-sources dir. No database is needed — the entities are never persisted,
 * only instantiated / mapped from a fake row.
 */
class GeneratedRepositoryTest {

    @Test
    void generatesRepositoryForRecordEntity() {
        BaseRepository<LedgerEntryProjection, java.util.UUID> repository = new BaseLedgerEntryProjectionRepository();

        assertThat(repository).isInstanceOf(BaseRepository.class);
    }

    @Test
    void generatesRepositoryForClassEntity() {
        BaseRepository<AuditNoteEntity, String> repository = new BaseAuditNoteRepository();

        assertThat(repository).isNotNull();
    }

    /**
     * The class-entity {@code mapRow} populates the entity through its setters (the fields are
     * private) — a regression here fails compilation of the generated source, and this test
     * additionally proves the values land.
     */
    @Test
    void mapRowUsesSettersForClassEntity() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        AuditNoteEntity entity = new BaseAuditNoteRepository().mapRow(fakeRow(Map.of(
                "note_id", "note-1",
                "note_text", "hello",
                "created_at", createdAt,
                "archived", true)));

        assertThat(entity.getId()).isEqualTo("note-1");
        assertThat(entity.getNoteText()).isEqualTo("hello");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.isArchived()).isTrue();
    }

    /**
     * Name-backed {@link Row} fake: r2dbc-spi 1.0.0 has exactly three abstract methods and the
     * mapper only uses the name-based reads; absent keys read as null, like a real driver.
     */
    private static Row fakeRow(Map<String, Object> values) {
        return new Row() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(String name, Class<T> type) {
                return (T) values.get(name);
            }

            @Override
            public <T> T get(int index, Class<T> type) {
                throw new UnsupportedOperationException("name-based access only");
            }

            @Override
            public RowMetadata getMetadata() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
