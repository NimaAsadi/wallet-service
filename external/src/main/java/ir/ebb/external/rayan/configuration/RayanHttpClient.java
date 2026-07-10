package ir.ebb.external.rayan.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.ebb.external.rayan.login.dto.request.RayanLoginRequest;
import ir.ebb.external.rayan.login.dto.response.RayanLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ClassicActorSystemProvider;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpEntities;
import org.apache.pekko.http.javadsl.model.HttpHeader;
import org.apache.pekko.http.javadsl.model.headers.RawHeader;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.HttpEntity;

/**
 * Pekko HTTP client replacing the Spring {@code RestClient} gateways. Performs
 * the three Rayan endpoints synchronously (blocks on the request future). On a
 * non-2xx response it throws {@link RayanHttpException} carrying the status and
 * body so {@link RayanResult} can apply token-refresh / error-mapping.
 */
@Slf4j
public class RayanHttpClient {

    public static final String HEADER_NAME_TOKEN = "Authorization";

    private final ClassicActorSystemProvider system;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public RayanHttpClient(ClassicActorSystemProvider system, String baseUrl, ObjectMapper objectMapper) {
        this.system = system;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    /** POST /authenticate → access token + ttl. */
    public RayanLoginResponse authenticate(RayanLoginRequest request) {
        String json = writeJson(request);
        log.atInfo().log("POST {}/authenticate", baseUrl);
        String body = send(HttpRequest.POST(baseUrl + "/authenticate")
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, json)));
        return readJson(body, RayanLoginResponse.class);
    }

    /** GET /customers/remainSettlementOfNextBusinessDay?dsCode= → CSV body. */
    public String getAllWallets(int dsCode, String token) {
        String url = baseUrl + "/customers/remainSettlementOfNextBusinessDay?dsCode=" + dsCode;
        log.atInfo().log("GET {}", url);
        return send(HttpRequest.GET(url).addHeader(auth(token)));
    }

    /** PUT /customers/initialCredit?credit=&dsName=&dbsAccountNumber= → body. */
    public String initCredit(String token, long credit, int dsName, long dbsAccountNumber) {
        String url = baseUrl + "/customers/initialCredit?credit=" + credit + "&dsName=" + dsName
                + "&dbsAccountNumber=" + dbsAccountNumber;
        log.atInfo().log("PUT {}", url);
        return send(HttpRequest.PUT(url).addHeader(auth(token)));
    }

    private String send(HttpRequest request) {
        HttpResponse response = Http.get(system)
                .singleRequest(request)
                .toCompletableFuture()
                .join();
        String body = bodyString(response);
        int status = response.status().intValue();
        if (status < 200 || status >= 300) {
            throw new RayanHttpException(status, body);
        }
        return body;
    }

    private String bodyString(HttpResponse response) {
        try {
            HttpEntity.Strict strict = response.entity().toStrict(30_000L, system)
                    .toCompletableFuture().join();
            return strict.getData().decodeString("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Rayan response body", e);
        }
    }

    private static HttpHeader auth(String token) {
        return RawHeader.create(HEADER_NAME_TOKEN, token);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Rayan request", e);
        }
    }

    private <T> T readJson(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize Rayan response: " + body, e);
        }
    }
}
