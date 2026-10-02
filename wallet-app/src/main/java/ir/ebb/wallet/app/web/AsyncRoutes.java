package ir.ebb.wallet.app.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.ebb.base.web.ResponseHelper;
import ir.ebb.common.dto.response.BaseErrorResponse;
import ir.ebb.common.dto.response.ErrorResponse;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.exception.handler.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpEntities;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * Bridges {@code CompletionStage}-returning services into Pekko HTTP routes while preserving the
 * original error contract. Pekko's {@code completeWithFuture} does NOT route future failures
 * through the route {@code ExceptionHandler}, so the {@code BusinessException} → 406 (with the
 * domain code in the body) mapping happens here, on the error channel of the stage.
 */
@Slf4j
public final class AsyncRoutes {

    private AsyncRoutes() {
    }

    /** Wraps {@code body} (on success) in {@code ResponseHelper.success}, mapping failures per contract. */
    public static CompletionStage<HttpResponse> complete(ObjectMapper mapper, StatusCode successStatus, CompletionStage<?> stage) {
        return stage
                .thenApply(body -> write(mapper, successStatus, ResponseHelper.success(body)))
                .exceptionally(error -> errorResponse(mapper, rootCause(error)));
    }

    /** The {@code run(...)} contract: 201 CREATED with a bare {@code success(201)} envelope. */
    public static CompletionStage<HttpResponse> completeCreatedAction(ObjectMapper mapper, CompletionStage<?> stage) {
        return stage
                .thenApply(__ -> write(mapper, StatusCodes.CREATED, ResponseHelper.success(201)))
                .exceptionally(error -> errorResponse(mapper, rootCause(error)));
    }

    public static HttpResponse errorResponse(ObjectMapper mapper, Throwable t) {
        if (t instanceof BusinessException be)
            return write(mapper, StatusCodes.NOT_ACCEPTABLE,
                    new BaseErrorResponse(new ErrorResponse(be.getMessage(), be.getCode())));
        if (t instanceof NotFoundException nfe)
            return write(mapper, StatusCodes.NOT_FOUND,
                    new BaseErrorResponse(new ErrorResponse(nfe.getMessage(), nfe.getCode())));
        log.error("Unhandled async route error", t);
        return write(mapper, StatusCodes.INTERNAL_SERVER_ERROR,
                new BaseErrorResponse(new ErrorResponse("Internal server error", 5000)));
    }

    /** Strips {@code CompletionException}/{@code ExecutionException} layers; keeps the root cause. */
    public static Throwable rootCause(Throwable error) {
        Throwable t = error;
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private static HttpResponse write(ObjectMapper mapper, StatusCode status, Object body) {
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            json = "{\"message\":\"serialization error\"}";
        }
        return HttpResponse.create()
                .withStatus(status)
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, json));
    }
}
