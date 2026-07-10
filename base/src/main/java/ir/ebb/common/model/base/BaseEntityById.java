package ir.ebb.common.model.base;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR (was a Hibernate
 * {@code @MappedSuperclass} with {@code @Id}/@Version}).
 * Plain base: a time-ordered UUID id is generated on construction (matching the
 * original no-arg-ctor behaviour); {@code version} backs the manual optimistic
 * lock in {@code WalletRepository.updateWalletNative}. Repositories override
 * both when loading existing rows from the DB.
 */
@Getter
@Setter
public class BaseEntityById extends BaseEntity {
    protected UUID id = UuidCreator.getTimeOrderedEpoch();
    protected long version = 0L;
}
