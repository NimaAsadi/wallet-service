package ir.ebb.wallet.service;

import ir.ebb.common.exception.handler.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletErrorsTest {

    @Test
    void unwrap_throwsTheRootBusinessExceptionFromAFailedStage() {
        BusinessException original = new BusinessException("Insufficient balance", 4005);
        CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(new CompletionException(original));

        // The re-thrown cause propagates raw through stage continuations (HTTP/gRPC observe it
        // via exceptionally/whenComplete); join() wraps it in CompletionException one more time.
        assertThatThrownBy(() -> WalletErrors.unwrap(failed).toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasCause(original);
    }

    @Test
    void unwrap_passesValuesThrough() {
        assertThat(WalletErrors.unwrap(CompletableFuture.completedFuture("ok"))
                .toCompletableFuture().join()).isEqualTo("ok");
    }

    @Test
    void rootCause_stripsCompletionExceptionLayers() {
        BusinessException original = new BusinessException("Wallet not found", 4001);
        CompletionException wrapped = new CompletionException(new CompletionException(original));

        assertThat(WalletErrors.rootCause(wrapped)).isSameAs(original);
    }

    @Test
    void rootCause_wrapsCheckedRootsInIllegalState() {
        Exception checked = new Exception("io");
        RuntimeException result = WalletErrors.rootCause(new CompletionException(checked));

        assertThat(result).isInstanceOf(IllegalStateException.class);
        assertThat(result.getCause()).isSameAs(checked);
    }
}
