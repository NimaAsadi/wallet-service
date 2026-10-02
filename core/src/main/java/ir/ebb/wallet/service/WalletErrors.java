package ir.ebb.wallet.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletionStage;

/**
 * Normalizes the exception wrappers {@code CompletionStage} / {@code askWithStatus} add around
 * domain errors, so callers (HTTP exception handler, gRPC {@code toStatus}, bidar-deposit code
 * mapping) can match on the raw {@code BusinessException} instead of its wrappers.
 */
public final class WalletErrors {

    private WalletErrors() {
    }

    /**
     * Re-throws the root cause on a failed stage so callers see the raw
     * {@code BusinessException} (unchecked) instead of a wrapped {@code CompletionException}.
     */
    public static <T> CompletionStage<T> unwrap(CompletionStage<T> stage) {
        return stage.handle((value, error) -> {
            if (error == null) return value;
            throw rootCause(error);
        });
    }

    /** Strips {@code CompletionException}/{@code ExecutionException} layers; keeps the root cause. */
    public static RuntimeException rootCause(Throwable error) {
        Throwable t = error;
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalStateException(t.getMessage(), t);
    }

    /** Convenience for {@code .thenCompose(v -> ...)} continuations that start from {@code Done}. */
    public static CompletionStage<Void> voidStage() {
        return CompletableFuture.completedFuture(null);
    }
}
