package ir.ebb.wallet.app.user.config;

import ir.ebb.common.configuration.security.user.jwt.BaseSecurityJWTConfiguration;
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
 * Secures /v1/user/** endpoints using the USER Keycloak realm.
 * Regular trading users authenticate against this realm.
 */
@Configuration
@Order(1)
@EnableWebSecurity
@EnableMethodSecurity
public class UserSecurityConfig extends BaseSecurityJWTConfiguration {

    @Value("${keycloak.user.jwk-set-uri}")
    private String userJwkSetUri;

    @Bean("userJwtDecoder")
    public JwtDecoder userJwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(userJwkSetUri).build();
    }

    @Bean("userFilterChain")
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {
        baseFilterChain(http);
        http.securityMatcher("/v1/user/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/user/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(userJwtDecoder())
                                .jwtAuthenticationConverter(jwtConverter())
                        )
                );
        return http.build();
    }

    private JwtAuthenticationConverter jwtConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthorityPrefix("PERMISSION_");
        gac.setAuthoritiesClaimName("authorities");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(gac);
        return converter;
    }
}
