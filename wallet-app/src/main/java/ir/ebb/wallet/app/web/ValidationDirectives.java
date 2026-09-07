package ir.ebb.wallet.app.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.ebb.base.exception.ExceptionConstants;
import ir.ebb.common.dto.response.BaseErrorResponse;
import ir.ebb.common.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpEntities;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.Route;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Comparator.comparing;
import static org.apache.pekko.http.javadsl.server.Directives.complete;

/**
 * Request-body validation utility: validates an unmarshalled DTO with the Jakarta
 * Bean Validation {@link Validator}; on violation completes with 400 and one
 * {@link ErrorResponse} per violation ("field: message", code
 * {@link ExceptionConstants#INVALID_REQUEST}), otherwise runs the inner route.
 *
 * <p>Static (not a {@link WalletHttpServer} method or a per-server instance) so it is
 * testable without the server's constructor graph and callable directly by any route —
 * {@link BaseController} subclasses pass the Dagger-injected {@link Validator} (and
 * {@link ObjectMapper}) at the call site.
 */
public final class ValidationDirectives {

    private ValidationDirectives() {
    }

    /**
     * Named {@code validateBody}, not {@code validate} — {@code Directives} already has
     * {@code validate(BooleanSupplier, String, Supplier)}.
     */
    public static <T> Route validateBody(Validator validator, ObjectMapper objectMapper, T body, Supplier<Route> inner) {
        Set<ConstraintViolation<T>> violations = validator.validate(body);
        if (violations.isEmpty()) {
            return inner.get();
        }
        List<ErrorResponse> errors = violations.stream()
                .sorted(comparing((ConstraintViolation<T> v) -> v.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .map(v -> new ErrorResponse(v.getPropertyPath() + ": " + v.getMessage(),
                        ExceptionConstants.INVALID_REQUEST.getCode()))
                .toList();
        return completeJson(objectMapper, StatusCodes.BAD_REQUEST, new BaseErrorResponse(errors));
    }

    private static Route completeJson(ObjectMapper objectMapper, StatusCode status, Object body) {
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            json = "{\"message\":\"serialization error\"}";
        }
        return complete(status, HttpEntities.create(ContentTypes.APPLICATION_JSON, json));
    }
}
