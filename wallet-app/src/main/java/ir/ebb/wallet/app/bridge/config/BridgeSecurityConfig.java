package ir.ebb.wallet.app.bridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secures /v1/bridge/** (REST) using the BRIDGE Keycloak realm.
 * gRPC endpoints are secured by {@link BridgeGrpcAuthInterceptor} using the same realm.
 */
@Configuration
@Order(3)
public class BridgeSecurityConfig {

    @Value("${keycloak.bridge.jwk-set-uri}")
    private String bridgeJwkSetUri;

    @Bean("bridgeJwtDecoder")
    public JwtDecoder bridgeJwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(bridgeJwkSetUri).build();
    }

    @Bean("bridgeFilterChain")
    public SecurityFilterChain bridgeFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/v1/bridge/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(bridgeJwtDecoder())
                                .jwtAuthenticationConverter(bridgeJwtConverter())
                        )
                )
                .build();
    }

    private JwtAuthenticationConverter bridgeJwtConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthorityPrefix("PERMISSION_");
        gac.setAuthoritiesClaimName("authorities");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(gac);
        return converter;
    }
}
