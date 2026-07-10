package ir.ebb.common.model.base;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR (was a Hibernate
 * {@code @MappedSuperclass} with {@code @CreationTimestamp}/@UpdateTimestamp}).
 * Plain base: audit timestamps are now set explicitly by the JDBC repositories
 * (or rely on the schema's {@code now()} defaults on insert).
 */
@Getter
@Setter
public class BaseEntity {
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
}
