package ir.ebb.wallet.app.web;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import ir.ebb.base.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Verifies a Keycloak-issued JWT against a realm's JWKS and builds a
 * {@link UserPrincipal}. Replaces Spring Security's {@code NimbusJwtDecoder} +
 * the per-realm filter chains. Returns {@code null} on any verification failure.
 *
 * <p>Claims read: {@code sub} (keycloakId), {@code dbs_account_number} (account),
 * {@code preferred_username} (name), {@code authorities} (permission list).</p>
 */
@Slf4j
public class JwtVerifier {

    private final DefaultJWTProcessor<SecurityContext> processor;

    public JwtVerifier(String jwkSetUri) {
        try {
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwkSetUri));
            JWSVerificationKeySelector<SecurityContext> selector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            this.processor = new DefaultJWTProcessor<>();
            this.processor.setJWSKeySelector(selector);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init JWT verifier for " + jwkSetUri, e);
        }
    }

    public UserPrincipal verify(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authorizationHeader.substring(7);
            JWTClaimsSet claims = processor.process(token, null);
            UUID keycloakId = claims.getSubject() != null ? UUID.fromString(claims.getSubject()) : null;
            Long dbsAccountNumber = claims.getLongClaim("dbs_account_number");
            String name = claims.getStringClaim("preferred_username");
            List<String> authorities = claims.getStringListClaim("authorities");
            Set<String> auths = authorities == null ? Set.of() : new HashSet<>(authorities);
            return new UserPrincipal(keycloakId, dbsAccountNumber, name, auths);
        } catch (Exception e) {
            log.debug("JWT verification failed: {}", e.getMessage());
            return null;
        }
    }
}
