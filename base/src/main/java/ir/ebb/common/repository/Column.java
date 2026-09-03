package ir.ebb.common.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to customize column mapping for entity fields in repository generation.
 *
 * Usage:
 * <pre>
 * {@code
 * public record OrderEntity(
 *     @Column(name = "order_id", primaryKey = true) String orderId,
 *     @Column(name = "account_num") String accountNumber,
 *     @Column(insertable = false, updatable = false) LocalDateTime createdAt
 * ) { }
 * }
 * </pre>
 */
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface Column {

    /**
     * The database column name.
     * If not specified, defaults to converting camelCase to snake_case.
     */
    String name() default "";

    /**
     * Whether this column is a primary key.
     * Primary key columns are excluded from UPDATE statements.
     */
    boolean primaryKey() default false;

    /**
     * Whether this column should be included in INSERT statements.
     * Useful for auto-generated columns or read-only fields.
     */
    boolean insertable() default true;

    /**
     * Whether this column should be included in UPDATE statements.
     * Useful for immutable fields or auto-managed columns.
     */
    boolean updatable() default true;

    /**
     * Whether this column can be null in the database.
     * Used for validation and query generation.
     */
    boolean nullable() default true;
}
