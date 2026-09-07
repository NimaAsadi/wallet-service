package ir.ebb.wallet.app.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ir.ebb.common.dto.response.BaseErrorResponse;
import ir.ebb.wallet.app.admin.dto.request.WalletInitCreditRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletDepositRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletFreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletSpendRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletUnfreezeRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.Route;
import org.apache.pekko.http.javadsl.testkit.JUnitRouteTest;
import org.apache.pekko.http.javadsl.testkit.TestRoute;
import org.apache.pekko.http.javadsl.testkit.TestRouteResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationDirectivesTest extends JUnitRouteTest {

    /** Same mapper shape as {@code ConfigModule.objectMapper()}. */
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final UUID TRACKING = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ValidationDirectives validation = new ValidationDirectives(MAPPER, VALIDATOR);

    /**
     * {@link JUnitRouteTest} wires its {@code ActorSystem} through a JUnit&nbsp;4 rule
     * ({@code ExternalResource}), which the Jupiter engine never invokes — so the
     * resource lifecycle is driven explicitly here.
     */
    @BeforeEach
    void startActorSystem() {
        systemResource().before();
    }

    @AfterEach
    void stopActorSystem() {
        systemResource().after();
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    /** Route under test: validates the fixture body, then answers "passed". */
    private TestRoute routeFor(Object body) {
        return testRoute(validation.validateBody(body, () -> complete(StatusCodes.OK, "passed")));
    }

    private BaseErrorResponse errorsOf(TestRouteResult result) throws Exception {
        result.assertStatusCode(StatusCodes.BAD_REQUEST);
        return MAPPER.readValue(result.entityString(), BaseErrorResponse.class);
    }

    static Stream<Arguments> bidarDtosWithZeroAmount() {
        return Stream.of(
                Arguments.of("deposit", new BidarDepositWalletDepositRequestDTO(1L, 0L, TRACKING)),
                Arguments.of("freeze", new BidarDepositWalletFreezeRequestDTO(1L, 0L, TRACKING)),
                Arguments.of("unfreeze", new BidarDepositWalletUnfreezeRequestDTO(1L, 0L, TRACKING)),
                Arguments.of("withdraw", new BidarDepositWalletSpendRequestDTO(1L, 0L, TRACKING)));
    }

    static Stream<Arguments> bidarDtosWithNegativeAmount() {
        return Stream.of(
                Arguments.of("deposit", new BidarDepositWalletDepositRequestDTO(1L, -1L, TRACKING)),
                Arguments.of("freeze", new BidarDepositWalletFreezeRequestDTO(1L, -1L, TRACKING)),
                Arguments.of("unfreeze", new BidarDepositWalletUnfreezeRequestDTO(1L, -1L, TRACKING)),
                Arguments.of("withdraw", new BidarDepositWalletSpendRequestDTO(1L, -1L, TRACKING)));
    }

    // ── bidar-deposit DTOs ────────────────────────────────────────────────────

    /** 0 stays accepted on all four endpoints (the old hand-rolled check allowed it too). */
    @ParameterizedTest(name = "{0} accepts amount 0")
    @MethodSource("bidarDtosWithZeroAmount")
    void zeroAmountPassesValidation(String endpoint, Object dto) {
        routeFor(dto).run(HttpRequest.POST("/")).assertStatusCode(StatusCodes.OK);
    }

    @ParameterizedTest(name = "{0} rejects amount -1")
    @MethodSource("bidarDtosWithNegativeAmount")
    void negativeAmountRejected(String endpoint, Object dto) throws Exception {
        BaseErrorResponse errors = errorsOf(routeFor(dto).run(HttpRequest.POST("/")));
        assertThat(errors.errors()).hasSize(1);
        assertThat(errors.errors().get(0).code()).isEqualTo(4009);
        assertThat(errors.errors().get(0).message()).isEqualTo("requestAmount: must be greater than or equal to 0");
    }

    @Test
    void allNullDepositReportsEveryViolationSorted() throws Exception {
        BaseErrorResponse errors = errorsOf(routeFor(
                new BidarDepositWalletDepositRequestDTO(null, null, null)).run(HttpRequest.POST("/")));
        assertThat(errors.errors()).hasSize(3);
        assertThat(errors.errors()).allSatisfy(e -> assertThat(e.code()).isEqualTo(4009));
        // sorted by property path → deterministic payload order
        assertThat(errors.errors().get(0).message()).startsWith("dbsAccountNumber:");
        assertThat(errors.errors().get(1).message()).startsWith("requestAmount:");
        assertThat(errors.errors().get(2).message()).startsWith("trackingId:");
    }

    // ── admin DTOs ────────────────────────────────────────────────────────────

    @Test
    void blankUserIdAndNullAccountNumberRejected() throws Exception {
        BaseErrorResponse errors = errorsOf(routeFor(new WalletRequestDTO(" ", null)).run(HttpRequest.POST("/")));
        assertThat(errors.errors()).hasSize(2);
        assertThat(errors.errors().get(0).message()).startsWith("dbsAccountNumber:");
        assertThat(errors.errors().get(1).message()).startsWith("userId:");
    }

    @Test
    void nestedWalletRequestDTOIsValidatedViaValidAnnotation() throws Exception {
        BaseErrorResponse errors = errorsOf(routeFor(WalletInitCreditRequestDTO.builder()
                .walletRequestDTO(new WalletRequestDTO("", null))
                .credit(null)
                .build()).run(HttpRequest.POST("/")));
        assertThat(errors.errors()).hasSize(3);
        // the nested path proves @Valid descended into walletRequestDTO
        assertThat(errors.errors().stream().map(e -> e.message()))
                .anySatisfy(m -> assertThat(m).isEqualTo("walletRequestDTO.userId: must not be blank"));
    }

    @Test
    void validCreditInitPassesValidation() {
        routeFor(WalletInitCreditRequestDTO.builder()
                .walletRequestDTO(new WalletRequestDTO("4b6f1006-7a5d-4a1a-9f5f-2f8f3f4e5d6b", 123L))
                .credit(100L)
                .build()).run(HttpRequest.POST("/")).assertStatusCode(StatusCodes.OK);
    }

    // ── BaseController reuse ──────────────────────────────────────────────────

    /** Mirrors the intended future-controller shape: holds a ValidationDirectives field. */
    private static final class AdminWalletController extends BaseController {
        private final ValidationDirectives validation;
        private final WalletRequestDTO request;

        private AdminWalletController(ValidationDirectives validation, WalletRequestDTO request) {
            this.validation = validation;
            this.request = request;
        }

        @Override
        public Route getRoute() {
            return path("wallet", () -> post(() ->
                    validation.validateBody(request, () -> complete(StatusCodes.CREATED, "created"))));
        }
    }

    @Test
    void baseControllerSubclassReusesValidationDirectives() {
        TestRoute valid = testRoute(new AdminWalletController(validation,
                new WalletRequestDTO("user", 1L)).getRoute());
        valid.run(HttpRequest.POST("/wallet")).assertStatusCode(StatusCodes.CREATED);

        TestRoute invalid = testRoute(new AdminWalletController(validation,
                new WalletRequestDTO(null, null)).getRoute());
        invalid.run(HttpRequest.POST("/wallet")).assertStatusCode(StatusCodes.BAD_REQUEST);
    }
}
