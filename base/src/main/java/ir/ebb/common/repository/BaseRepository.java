package ir.ebb.common.repository;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.Statement;
import org.apache.pekko.Done;
import org.apache.pekko.projection.r2dbc.javadsl.R2dbcSession;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Base interface for all generated repositories.
 * Provides common CRUD operations using R2DBC.
 *
 * @param <T> the entity type
 * @param <ID> the primary key type
 */
public interface BaseRepository<T, ID> {

    // ================== INSERT OPERATIONS ==================

    /**
     * Creates a prepared statement for inserting an entity.
     *
     * @param session the R2DBC session
     * @param entity the entity to insert
     * @return prepared statement for execution
     */
    Statement insertStatement(R2dbcSession session, T entity);

    /**
     * Inserts an entity to the database.
     *
     * @param session the R2DBC session
     * @param entity the entity to insert
     * @return completion stage with the number of affected rows
     */
    CompletionStage<Long> insert(R2dbcSession session, T entity);

    /**
     * Saves an entity to the database (alias for insert for backward compatibility).
     *
     * @param session the R2DBC session
     * @param entity the entity to save
     * @return completion stage that completes when save is done
     */
    CompletionStage<Done> save(R2dbcSession session, T entity);

    /**
     * Creates a prepared statement for saving an entity (alias for insertStatement for backward compatibility).
     *
     * @param session the R2DBC session
     * @param entity the entity to save
     * @return prepared statement for execution
     */
    Statement saveStatement(R2dbcSession session, T entity);

    // ================== UPDATE OPERATIONS ==================

    /**
     * Creates a prepared statement for updating an entity by its primary key.
     *
     * @param session the R2DBC session
     * @param entity the entity to update
     * @return prepared statement for execution
     */
    Statement updateStatement(R2dbcSession session, T entity);

    /**
     * Updates an entity in the database by its primary key.
     *
     * @param session the R2DBC session
     * @param entity the entity to update
     * @return completion stage with the number of affected rows
     */
    CompletionStage<Long> updateOne(R2dbcSession session, T entity);

    /**
     * Updates multiple entities in the database.
     *
     * @param session the R2DBC session
     * @param entities the entities to update
     * @return completion stage with a list of affected row counts
     */
    CompletionStage<List<Long>> update(R2dbcSession session, List<T> entities);

    // ================== SELECT OPERATIONS ==================

    /**
     * Creates a prepared statement for selecting an entity by its primary key.
     *
     * @param session the R2DBC session
     * @param id the primary key value
     * @return prepared statement for execution
     */
    Statement selectByIdStatement(R2dbcSession session, ID id);

    /**
     * Selects a single entity by its primary key.
     *
     * @param session the R2DBC session
     * @param id the primary key value
     * @return completion stage with an optional entity
     */
    CompletionStage<Optional<T>> selectOne(R2dbcSession session, ID id);

    /**
     * Creates a prepared statement for selecting all entities.
     *
     * @param session the R2DBC session
     * @return prepared statement for execution
     */
    Statement selectAllStatement(R2dbcSession session);

    /**
     * Selects all entities from the table.
     *
     * @param session the R2DBC session
     * @return completion stage with a list of entities
     */
    CompletionStage<List<T>> select(R2dbcSession session);

    // ================== DELETE OPERATIONS ==================

    /**
     * Creates a prepared statement for deleting an entity by its primary key.
     *
     * @param session the R2DBC session
     * @param id the primary key value
     * @return prepared statement for execution
     */
    Statement deleteByIdStatement(R2dbcSession session, ID id);

    /**
     * Deletes an entity by its primary key.
     *
     * @param session the R2DBC session
     * @param id the primary key value
     * @return completion stage with the number of affected rows
     */
    CompletionStage<Long> deleteById(R2dbcSession session, ID id);

    // ================== EXISTS OPERATIONS ==================

    /**
     * Creates a prepared statement for checking if an entity exists by its primary key.
     *
     * @param session the R2DBC session
     * @param id the primary key value
     * @return prepared statement for execution
     */
    Statement existsByIdStatement(R2dbcSession session, ID id);

    /**
     * Checks if an entity exists by its primary key.
     *
     * @param session the R2DBC session
     * @param id the primary key value
     * @return completion stage with true if entity exists, false otherwise
     */
    CompletionStage<Boolean> existsById(R2dbcSession session, ID id);

    // ================== ROW MAPPING ==================

    /**
     * Maps a database row to an entity instance.
     *
     * @param row the database row
     * @return the mapped entity
     */
    T mapRow(Row row);
}
