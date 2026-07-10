package ir.ebb.external.rayan.configuration;

/**
 * Unchecked exception carrying a non-2xx Rayan HTTP response (status + body).
 * Thrown by {@link RayanHttpClient}; handled by {@link RayanResult} to drive
 * token-refresh-on-401 and structured-error parsing.
 */
public class RayanHttpException extends RuntimeException {

    private final int status;
    private final String body;

    public RayanHttpException(int status, String body) {
        super("Rayan HTTP " + status + ": " + body);
        this.status = status;
        this.body = body;
    }

    public int status() {
        return status;
    }

    public String body() {
        return body;
    }
}
