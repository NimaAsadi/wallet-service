package ir.ebb.wallet.service;

import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the executor that may run blocking I/O (the {@code wallet-blocking-dispatcher} in the
 * app module). JDBC read-side repositories are always invoked on it — never on a Pekko
 * dispatcher or inside an R2DBC continuation.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface BlockingExecutor {
}
