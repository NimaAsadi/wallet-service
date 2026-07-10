package ir.ebb.base.exception;

/**
 * Replaces Spring's {@code org.springframework.dao.OptimisticLockingFailureException}.
 * Thrown by {@code WalletRepository.updateWalletNative} when the version-checked
 * UPDATE affects 0 rows (a concurrent modification). The {@code DailyWalletActor}
 * catches it to reload-and-retry-once.
 */
public class OptimisticLockingFailureException extends RuntimeException {
    public OptimisticLockingFailureException(String message) {
        super(message);
    }
}
