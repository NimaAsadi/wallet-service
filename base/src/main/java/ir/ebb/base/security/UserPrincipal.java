package ir.ebb.base.security;

import ir.ebb.common.model.user.User;

import java.util.Set;
import java.util.UUID;

/**
 * Replaces the dropped {@code ir.ebb.common.utility.SecurityUtil} thread-local.
 * Populated by the Pekko HTTP JWT directive and carried as a request attribute.
 * Serves both the user audience (via {@link #asUser()}) and the admin audience
 * (via {@code keycloakId}/{@code name} for audit fields).
 */
public record UserPrincipal(UUID keycloakId, Long dbsAccountNumber, String name, Set<String> authorities) {

    public User asUser() {
        return User.of(keycloakId, dbsAccountNumber);
    }

    public boolean hasAuthority(String permission) {
        return authorities != null && authorities.contains(permission);
    }
}
