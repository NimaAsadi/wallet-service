package ir.ebb.common.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Re-homed from the dropped {@code ir.ebb:common} JAR (was a JPA {@code @Embeddable}).
 * Plain value type holding a user's Keycloak id and wallet account number.
 * Used as a HashMap key / Set member, so equals/hashCode cover both fields.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private UUID keycloakId;
    private Long dbsAccountNumber;

    public static User of(UUID keycloakId, Long dbsAccountNumber) {
        return new User(keycloakId, dbsAccountNumber);
    }
}
