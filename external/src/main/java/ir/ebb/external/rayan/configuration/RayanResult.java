package ir.ebb.external.rayan.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.external.rayan.dto.RayanErrorResponseDTO;
import ir.ebb.external.rayan.login.RayanLoginService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@Component
public class RayanResult<T> implements ApplicationContextAware {

    public static ObjectMapper objectMapper;
    public static RayanLoginService rayanLoginService;

    @Getter
    private boolean success;
    @Getter
    private boolean failure;
    private T successResult;
    private RayanErrorResponseDTO failureResult;

    private static <T> RayanResult<T> from(
            boolean success, boolean failure,
            T successResult, RayanErrorResponseDTO failureResult) {
        var result = new RayanResult<T>();
        result.success = success;
        result.failure = failure;
        result.successResult = successResult;
        result.failureResult = failureResult;
        return result;
    }

    public static <T> RayanResult<T> from(Supplier<T> apiCall) {
        try {
            return RayanResult.success(apiCall.get());
        } catch (HttpClientErrorException e) {
            return handleHttpClientErrorException(apiCall, e);
        } catch (Exception e) {
            log.error("Rayan http call unhandled exception:", e);
            throw new BusinessException(ExceptionConstants.INTERNAL_SERVER_ERROR.getMessage(),
                    ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    public static <T> RayanResult<T> success(T successResult) {
        return RayanResult.from(true, false, successResult, null);
    }

    public static <T> RayanResult<T> failure(RayanErrorResponseDTO failureResult) {
        return RayanResult.from(false, true, null, failureResult);
    }

    public RayanResult<T> onSuccess(Consumer<T> onSuccess) {
        if (success) onSuccess.accept(successResult);
        return this;
    }

    public <U> RayanResult<U> map(Function<? super T, U> mapper) {
        if (success) return success(mapper.apply(successResult));
        return failure(failureResult);
    }

    public RayanResult<T> onFailure(Consumer<RayanErrorResponseDTO> onFailure) {
        if (failure) onFailure.accept(failureResult);
        return this;
    }

    public <U> RayanResult<U> recover(Function<RayanErrorResponseDTO, U> errorMapper) {
        if (failure) return success(errorMapper.apply(failureResult));
        return (RayanResult<U>) this;
    }

    public T getOrElseThrow() {
        if (failure) throw new BusinessException(failureResult.description(), failureResult.errorCode());
        return get();
    }

    public T get() {
        return successResult;
    }

    private static <U> RayanResult<U> handleHttpClientErrorException(
            Supplier<U> apiCall, HttpClientErrorException e) {
        if (e.getStatusCode().is4xxClientError()) {
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.equals(UNAUTHORIZED)) {
                log.atError().log("UNAUTHORIZED from Rayan. Refreshing token and retrying...");
                rayanLoginService.login();
                return RayanResult.from(apiCall);
            } else if (statusCode.equals(BAD_REQUEST) || statusCode.equals(NOT_ACCEPTABLE)) {
                var body = e.getResponseBodyAsString();
                try {
                    var errorResponse = objectMapper.readValue(body, RayanErrorResponseDTO.class);
                    return RayanResult.failure(errorResponse);
                } catch (JsonProcessingException ex) {
                    log.atError().log(body, ex);
                    throw new BusinessException(ExceptionConstants.INTERNAL_SERVER_ERROR.getMessage(),
                            ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
                }
            } else {
                log.atError().log("Rayan http {} error: {}", e.getStatusCode(), e.getMessage());
                throw new BusinessException(ExceptionConstants.INTERNAL_SERVER_ERROR.getMessage(),
                        ExceptionConstants.INTERNAL_SERVER_ERROR.getCode());
            }
        }
        log.atError().log("Rayan http {} error: {}", e.getStatusCode(), e.getMessage());
        throw e;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        rayanLoginService = applicationContext.getBean(RayanLoginService.class);
        objectMapper = applicationContext.getBean(ObjectMapper.class);
    }
}
