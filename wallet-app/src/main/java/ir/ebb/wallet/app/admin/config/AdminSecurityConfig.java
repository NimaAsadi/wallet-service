package ir.ebb.wallet.app.admin.config;

import ir.ebb.common.configuration.security.admin.opaque.BaseSecurityOpaqueConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secures /v1/admin/** endpoints using the ADMIN Keycloak realm.
 * Backoffice operators authenticate against this separate realm.
 */
@Order(2)
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class AdminSecurityConfig extends BaseSecurityOpaqueConfiguration {

    @Value("${keycloak.admin.jwk-set-uri}")
    private String adminJwkSetUri;

    @Bean("adminJwtDecoder")
    public JwtDecoder adminJwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(adminJwkSetUri).build();
    }

    @Bean("adminFilterChain")
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        baseFilterChain(http);
        http.securityMatcher("/v1/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/admin/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(adminJwtDecoder())
                                .jwtAuthenticationConverter(adminJwtConverter())
                        )
                );
        return http.build();
    }

    private JwtAuthenticationConverter adminJwtConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthorityPrefix("PERMISSION_");
        gac.setAuthoritiesClaimName("authorities");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(gac);
        return converter;
    }
}
