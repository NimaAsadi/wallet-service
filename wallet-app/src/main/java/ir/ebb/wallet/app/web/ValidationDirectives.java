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
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Comparator.comparing;

/**
 * Request-body validation directive: validates an unmarshalled DTO with the Jakarta
 * Bean Validation {@link Validator}; on violation completes with 400 and one
 * {@link ErrorResponse} per violation ("field: message", code
 * {@link ExceptionConstants#INVALID_REQUEST}), otherwise runs the inner route.
 *
 * <p>Standalone (not a {@link WalletHttpServer} method) so it is testable without the
 * server's constructor graph and reusable by future {@link BaseController} subclasses,
 * which simply hold one as a field.
 */
public class ValidationDirectives extends AllDirectives {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ValidationDirectives(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Named {@code validateBody}, not {@code validate} — {@code Directives} already has
     * {@code validate(BooleanSupplier, String, Supplier)}.
     */
    public <T> Route validateBody(T body, Supplier<Route> inner) {
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
        return completeJson(StatusCodes.BAD_REQUEST, new BaseErrorResponse(errors));
    }

    private Route completeJson(StatusCode status, Object body) {
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            json = "{\"message\":\"serialization error\"}";
        }
        return complete(status, HttpEntities.create(ContentTypes.APPLICATION_JSON, json));
    }
}
