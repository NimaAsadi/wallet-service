package ir.ebb.wallet.app.di;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** {@link ir.ebb.wallet.app.web.JwtVerifier} for the admin audience (wallet.keycloak.admin.jwk-set-uri). */
@Qualifier
@Retention(RetentionPolicy.CLASS)
public @interface AdminJwtVerifier {
}
