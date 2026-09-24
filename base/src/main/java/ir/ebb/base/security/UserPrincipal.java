package ir.ebb.base.security;

import java.util.Set;

/**
 * Replaces the dropped {@code ir.ebb.common.utility.SecurityUtil} thread-local.
 * Populated by the Pekko HTTP JWT directive and carried as a request attribute.
 * Serves both the user audience (via {@link #asUser()}) and the admin audience
 * (via {@code keycloakId}/{@code name} for audit fields).
 */
public record UserPrincipal(Long accountNumber, String name, Set<String> authorities) {

    public long asUser() {
        return accountNumber;
    }

    public boolean hasAuthority(String permission) {
        return authorities != null && authorities.contains(permission);
    }
}
