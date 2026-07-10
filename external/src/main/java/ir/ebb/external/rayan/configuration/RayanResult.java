package ir.ebb.external.rayan.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.external.rayan.dto.RayanErrorResponseDTO;
import ir.ebb.external.rayan.login.RayanLoginService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Result-style wrapper around every Rayan HTTP call. Replaces the Spring
 * {@code RestClient}-based version: it now catches {@link RayanHttpException}
 * from {@link RayanHttpClient} and applies the same semantics — refresh the
 * token and retry once on 401, parse a structured error on 400/406, and surface
 * any other failure as {@code BusinessException(5000)}. Dependencies are wired
 * via {@link #configure(ObjectMapper, RayanLoginService)} from the bootstrap.
 */
@Slf4j
public class RayanResult<T> {

    private static ObjectMapper objectMapper;
    private static RayanLoginService rayanLoginService;

    public static void configure(ObjectMapper om, RayanLoginService loginService) {
        objectMapper = om;
        rayanLoginService = loginService;
    }

    @Getter
    private boolean success;
    @Getter
    private boolean failure;
    private T successResult;
    private RayanErrorResponseDTO failureResult;

    private RayanResult() {}

    private static <T> RayanResult<T> of(boolean success, boolean failure, T successResult, RayanErrorResponseDTO failureResult) {
        var result = new RayanResult<T>();
        result.success = success;
        result.failure = failure;
        result.successResult = successResult;
        result.failureResult = failureResult;
        return result;
    }

    public static <T> RayanResult<T> from(Supplier<T> apiCall) {
        try {
            return success(apiCall.get());
        } catch (RayanHttpException e) {
            return handleHttpException(apiCall, e);
        } catch (Exception e) {
            log.error("Rayan http call unhandled exception:", e);
            throw new BusinessException(ExceptionConstants.INTERNAL_SERVER_ERROR.getMessage(),
                    ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    public static <T> RayanResult<T> success(T successResult) {
        return RayanResult.of(true, false, successResult, null);
    }

    public static <T> RayanResult<T> failure(RayanErrorResponseDTO failureResult) {
        return RayanResult.of(false, true, null, failureResult);
    }

    public RayanResult<T> onSuccess(Consumer<T> onSuccess) {
        if (success) {
            onSuccess.accept(successResult);
        }
        return this;
    }

    public <U> RayanResult<U> map(Function<? super T, U> mapper) {
        if (success) {
            return success(mapper.apply(successResult));
        }
        return failure(failureResult);
    }

    public RayanResult<T> onFailure(Consumer<RayanErrorResponseDTO> onFailure) {
        if (failure) {
            onFailure.accept(failureResult);
        }
        return this;
    }

    public <U> RayanResult<U> recover(Function<RayanErrorResponseDTO, U> errorMapper) {
        if (failure) {
            return success(errorMapper.apply(failureResult));
        }
        return (RayanResult<U>) this;
    }

    public T getOrElseThrow() {
        if (failure) {
            throw new BusinessException(failureResult.description(), failureResult.errorCode());
        }
        return get();
    }

    public T get() {
        return successResult;
    }

    private static <T> RayanResult<T> handleHttpException(Supplier<T> apiCall, RayanHttpException e) {
        int status = e.status();
        if (status == 401) {
            log.atError().log("UNAUTHORIZED from Rayan. Refreshing token and retrying...");
            rayanLoginService.login();
            return RayanResult.from(apiCall);
        } else if (status == 400 || status == 406) {
            try {
                return RayanResult.failure(objectMapper.readValue(e.body(), RayanErrorResponseDTO.class));
            } catch (Exception ex) {
                log.atError().log("Failed to parse Rayan error body: {}", e.body(), ex);
                throw new BusinessException(ExceptionConstants.INTERNAL_SERVER_ERROR.getMessage(),
                        ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
            }
        } else if (status >= 400 && status < 500) {
            log.atError().log("Rayan http {} error: {}", status, e.body());
            throw new BusinessException(ExceptionConstants.INTERNAL_SERVER_ERROR.getMessage(),
                    ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
        }
        // 5xx and anything else: surface as internal error
        log.atError().log("Rayan http {} error: {}", status, e.body());
        throw new BusinessException(ExceptionConstants.INTERNAL_SERVER_ERROR.getMessage(),
                ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
    }
}
