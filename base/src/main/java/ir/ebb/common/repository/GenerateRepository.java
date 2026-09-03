package ir.ebb.common.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark entity records for automatic repository generation.
 *
 * Usage:
 * <pre>
 * {@code
 * @GenerateRepository(table = "orders")
 * public record OrderEntity(...) { ... }
 * }
 * </pre>
 *
 * This will generate a repository class named "BaseOrderRepository" by default.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateRepository {

    /**
     * The database table name for this entity.
     * If not specified, defaults to the entity class name in snake_case.
     */
    String table() default "";

    /**
     * Custom name for the generated repository class.
     * If not specified, defaults to removing "Entity" suffix and adding "Base" prefix + "Repository" suffix.
     * Example: "OrderEntity" -> "BaseOrderRepository"
     */
    String repositoryName() default "";

    /**
     * Custom package for the generated repository.
     * If not specified, defaults to the entity's package with a trailing ".entity" segment
     * replaced by ".repository" (or ".repository" appended when the package doesn't end in ".entity").
     */
    String packageName() default "";
}
