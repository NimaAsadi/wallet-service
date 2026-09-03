package ir.ebb.common.repository.test;

import ir.ebb.common.repository.BaseRepository;
import ir.ebb.common.repository.test.entity.AuditNoteEntity;
import ir.ebb.common.repository.test.entity.LedgerEntryProjection;
import ir.ebb.common.repository.test.repository.BaseAuditNoteRepository;
import ir.ebb.common.repository.test.repository.BaseLedgerEntryProjectionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof that {@link ir.ebb.common.repository.RepositoryProcessor} ran during test
 * compilation: the imports above only resolve because the processor generated
 * {@code BaseLedgerEntryProjectionRepository} / {@code BaseAuditNoteRepository} into the test
 * source set's generated-sources dir. No database is needed — the entities are never persisted,
 * only instantiated.
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
}
