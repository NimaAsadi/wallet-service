package ir.ebb.wallet.app.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.ebb.base.constant.ConstantTransformer;
import ir.ebb.base.dto.FixedConstantResponse;
import ir.ebb.base.security.UserPrincipal;
import ir.ebb.base.web.ResponseHelper;
import ir.ebb.common.dto.request.PaginatedRequestDTO;
import ir.ebb.common.dto.response.BaseErrorResponse;
import ir.ebb.common.dto.response.ErrorResponse;
import ir.ebb.common.exception.handler.BusinessException;
import ir.ebb.common.exception.handler.NotFoundException;
import ir.ebb.wallet.app.admin.dto.request.CreditHistorySearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletInitCreditRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletSearchRequestDTO;
import ir.ebb.wallet.app.admin.dto.request.WalletTransactionSearchRequestDTO;
import ir.ebb.wallet.app.admin.service.AdminWalletTransactionWebService;
import ir.ebb.wallet.app.admin.service.AdminWalletWebService;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletDepositRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletFreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletSpendRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.dto.request.BidarDepositWalletUnfreezeRequestDTO;
import ir.ebb.wallet.app.bridge.bidardeposit.service.BidarDepositWalletWebService;
import ir.ebb.wallet.app.bridge.dto.request.BridgeTurnoverSearchRequestDTO;
import ir.ebb.wallet.app.bridge.service.BridgeTurnoverWebService;
import ir.ebb.wallet.app.bridge.service.BridgeWalletWebService;
import ir.ebb.wallet.app.user.dto.request.TurnoverSearchRequestDTO;
import ir.ebb.wallet.app.user.service.TurnoverWebService;
import ir.ebb.wallet.app.user.service.WalletWebService;
import ir.ebb.wallet.constant.enumeration.RayanCreditStatus;
import ir.ebb.wallet.constant.enumeration.TurnoverOperationType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpEntities;
import org.apache.pekko.http.javadsl.model.HttpHeader;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.ExceptionHandler;
import org.apache.pekko.http.javadsl.server.Route;

import ir.ebb.wallet.app.di.AdminJwtVerifier;
import ir.ebb.wallet.app.di.BridgeJwtVerifier;
import ir.ebb.wallet.app.di.UserJwtVerifier;

import jakarta.validation.Validator;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.pekko.http.javadsl.server.Directives.*;
import static org.apache.pekko.http.javadsl.server.PathMatchers.segment;

/**
 * Pekko HTTP server replacing the 6 Spring {@code @RestController}s. One route
 * tree with per-audience JWT auth, Jackson JSON, and an exception handler that
 * preserves the original contract (BusinessException → 406 with the domain code
 * in the body, not as the HTTP status).
 */
@Slf4j
@Singleton
public class WalletHttpServer extends AllDirectives {

    private final ObjectMapper objectMapper;
    private final ValidationDirectives validation;
    private final JwtVerifier userVerifier;
    private final JwtVerifier adminVerifier;
    private final JwtVerifier bridgeVerifier;

    private final WalletWebService walletWebService;
    private final TurnoverWebService turnoverWebService;
    private final BridgeWalletWebService bridgeWalletWebService;
    private final BridgeTurnoverWebService bridgeTurnoverWebService;
    private final BidarDepositWalletWebService bidarDepositWalletWebService;
    private final AdminWalletWebService adminWalletWebService;
    private final AdminWalletTransactionWebService adminWalletTransactionWebService;

    @Inject
    public WalletHttpServer(ObjectMapper objectMapper, Validator validator,
                            @UserJwtVerifier JwtVerifier userVerifier,
                            @AdminJwtVerifier JwtVerifier adminVerifier,
                            @BridgeJwtVerifier JwtVerifier bridgeVerifier,
                            WalletWebService walletWebService, TurnoverWebService turnoverWebService,
                            BridgeWalletWebService bridgeWalletWebService, BridgeTurnoverWebService bridgeTurnoverWebService,
                            BidarDepositWalletWebService bidarDepositWalletWebService,
                            AdminWalletWebService adminWalletWebService,
                            AdminWalletTransactionWebService adminWalletTransactionWebService) {
        this.objectMapper = objectMapper;
        this.validation = new ValidationDirectives(objectMapper, validator);
        this.userVerifier = userVerifier;
        this.adminVerifier = adminVerifier;
        this.bridgeVerifier = bridgeVerifier;
        this.walletWebService = walletWebService;
        this.turnoverWebService = turnoverWebService;
        this.bridgeWalletWebService = bridgeWalletWebService;
        this.bridgeTurnoverWebService = bridgeTurnoverWebService;
        this.bidarDepositWalletWebService = bidarDepositWalletWebService;
        this.adminWalletWebService = adminWalletWebService;
        this.adminWalletTransactionWebService = adminWalletTransactionWebService;
    }

    public Route routes() {
        return handleExceptions(exceptionHandler(), () -> concat(
                pathPrefix("v1", () -> concat(
                        pathPrefix("user", () -> authenticate(userVerifier, this::userRoutes)),
                        pathPrefix("bridge", () -> authenticate(bridgeVerifier, this::bridgeRoutes)),
                        pathPrefix("admin", () -> authenticate(adminVerifier, this::adminRoutes))
                ))
        ));
    }

    // ── user audience ─────────────────────────────────────────────────────────

    private Route userRoutes(UserPrincipal principal) {
        return concat(
                path("wallet", () -> get(() ->
                        ok(ResponseHelper.success(walletWebService.getWalletDetails(principal))))),
                pathPrefix("turnover", () -> concat(
                        path("today", () -> get(() ->
                                ok(ResponseHelper.success(turnoverWebService.getTodayTurnover(principal))))),
                        path("types", () -> get(() -> ok(ResponseHelper.success(turnoverTypes())))),
                        pathEnd(() -> get(() -> parameterMap(params ->
                                ok(ResponseHelper.success(turnoverWebService.getHistory(principal, bindTurnoverSearch(params)))))))
                ))
        );
    }

    // ── bridge audience ───────────────────────────────────────────────────────

    private Route bridgeRoutes(UserPrincipal principal) {
        return concat(
                pathPrefix("wallet", () -> pathPrefix("account-number", () -> path(segment(), acct -> get(() ->
                        ok(ResponseHelper.success(bridgeWalletWebService.getWalletDetails(Long.parseLong(acct)))))))),
                pathPrefix("turnover", () -> concat(
                        path("types", () -> get(() -> ok(ResponseHelper.success(turnoverTypes())))),
                        path(segment(), acct -> concat(
                                path("today", () -> get(() ->
                                        ok(ResponseHelper.success(bridgeTurnoverWebService.getTodayTurnover(Long.parseLong(acct)))))),
                                pathEnd(() -> get(() -> parameterMap(params ->
                                        ok(ResponseHelper.success(bridgeTurnoverWebService.getHistory(
                                                Long.parseLong(acct), bindBridgeTurnoverSearch(params)))))))
                        ))
                )),
                pathPrefix("bidar-deposit", () -> concat(
                        path("deposit", () -> post(() -> entity(
                                Jackson.unmarshaller(objectMapper, BidarDepositWalletDepositRequestDTO.class),
                                req -> validation.validateBody(req,
                                        () -> created(ResponseHelper.success(bidarDepositWalletWebService.deposit(req), 201)))))),
                        path("freeze", () -> post(() -> entity(
                                Jackson.unmarshaller(objectMapper, BidarDepositWalletFreezeRequestDTO.class),
                                req -> validation.validateBody(req,
                                        () -> created(ResponseHelper.success(bidarDepositWalletWebService.freeze(req), 201)))))),
                        path("unfreeze", () -> post(() -> entity(
                                Jackson.unmarshaller(objectMapper, BidarDepositWalletUnfreezeRequestDTO.class),
                                req -> validation.validateBody(req,
                                        () -> created(ResponseHelper.success(bidarDepositWalletWebService.unfreeze(req), 201)))))),
                        path("withdraw", () -> post(() -> entity(
                                Jackson.unmarshaller(objectMapper, BidarDepositWalletSpendRequestDTO.class),
                                req -> validation.validateBody(req,
                                        () -> created(ResponseHelper.success(bidarDepositWalletWebService.spend(req), 201))))))
                ))
        );
    }

    // ── admin audience ────────────────────────────────────────────────────────

    private Route adminRoutes(UserPrincipal principal) {
        return concat(
                pathPrefix("wallet", () -> concat(
                        pathEnd(() -> concat(
                                post(() -> entity(Jackson.unmarshaller(objectMapper, WalletRequestDTO.class), req ->
                                        requireAuthority(principal, "PERMISSION_USERS_UPDATE", () ->
                                                validation.validateBody(req, () ->
                                                        created(run(() -> adminWalletWebService.create(req.userId(), req.dbsAccountNumber()))))))),
                                get(() -> parameterMap(params -> requireAuthority(principal, "PERMISSION_CUSTOMERWALLET_VIEW", () ->
                                        ok(ResponseHelper.success(adminWalletWebService.searchWallet(bindWalletSearch(params)))))))
                        )),
                        pathPrefix("rayan", () -> path(segment(), acct -> get(() ->
                                requireAuthority(principal, "PERMISSION_CUSTOMERWALLET_VIEW", () ->
                                        ok(ResponseHelper.success(adminWalletWebService.getRayanWallet(Long.parseLong(acct)))))))),
                        pathPrefix("credit", () -> concat(
                                path("init", () -> put(() -> entity(
                                        Jackson.unmarshaller(objectMapper, WalletInitCreditRequestDTO.class), req ->
                                                requireAuthorityAll(principal, "PERMISSION_CREDITS_ADD", "PERMISSION_USERS_SEARCH", () ->
                                                        validation.validateBody(req, () ->
                                                                created(run(() -> adminWalletWebService.initCredit(req, principal)))))))),
                                path("remove", () -> put(() -> entity(
                                        Jackson.unmarshaller(objectMapper, WalletRequestDTO.class), req ->
                                                requireAuthorityAll(principal, "PERMISSION_CREDITS_UPDATE", "PERMISSION_USERS_SEARCH", () ->
                                                        validation.validateBody(req, () ->
                                                                created(run(() -> adminWalletWebService.removeCredit(req, principal)))))))),
                                path("history", () -> get(() -> parameterMap(params ->
                                        requireAuthorityAll(principal, "PERMISSION_CREDITS_VIEW", "PERMISSION_USERS_SEARCH", () ->
                                                ok(ResponseHelper.success(adminWalletWebService.searchCredit(bindCreditHistorySearch(params)))))))),
                                path("status", () -> get(() -> requireAuthority(principal, "PERMISSION_CREDITS_VIEW", () ->
                                        ok(ResponseHelper.success(Arrays.stream(RayanCreditStatus.values())
                                                .map(ConstantTransformer::enumToFixedConstantResponse).toList())))))
                        ))
                )),
                path("wallet-transaction", () -> get(() -> parameterMap(params ->
                        requireAuthority(principal, "PERMISSION_CREDITS_VIEW", () ->
                                ok(ResponseHelper.success(adminWalletTransactionWebService.searchWalletTransaction(
                                        bindWalletTransactionSearch(params))))))))
        );
    }

    // ── auth + response helpers ───────────────────────────────────────────────

    private Route authenticate(JwtVerifier verifier, Function<UserPrincipal, Route> inner) {
        return extractRequest(req -> {
            String header = req.getHeader("Authorization").map(HttpHeader::value).orElse(null);
            UserPrincipal principal = verifier.verify(header);
            return principal == null ? complete(StatusCodes.UNAUTHORIZED) : inner.apply(principal);
        });
    }

    private Route requireAuthority(UserPrincipal principal, String permission, Supplier<Route> inner) {
        return principal.hasAuthority(permission) ? inner.get() : complete(StatusCodes.FORBIDDEN);
    }

    private Route requireAuthorityAll(UserPrincipal principal, String a, String b, Supplier<Route> inner) {
        return (principal.hasAuthority(a) && principal.hasAuthority(b)) ? inner.get() : complete(StatusCodes.FORBIDDEN);
    }

    /** Runs a void action and returns a 201 CREATED envelope, letting exceptions reach the handler. */
    private Route run(Runnable action) {
        action.run();
        return completeJson(StatusCodes.CREATED, ResponseHelper.success(201));
    }

    private Route ok(Object body) {
        return completeJson(StatusCodes.OK, body);
    }

    private Route created(Object body) {
        return completeJson(StatusCodes.CREATED, body);
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

    private ExceptionHandler exceptionHandler() {
        return ExceptionHandler.newBuilder()
                .match(BusinessException.class, e -> completeJson(StatusCodes.NOT_ACCEPTABLE,
                        new BaseErrorResponse(new ErrorResponse(e.getMessage(), e.getCode()))))
                .match(NotFoundException.class, e -> completeJson(StatusCodes.NOT_FOUND,
                        new BaseErrorResponse(new ErrorResponse(e.getMessage(), e.getCode()))))
                .matchAny(e -> {
                    log.error("Unhandled HTTP error", e);
                    return completeJson(StatusCodes.INTERNAL_SERVER_ERROR,
                            new BaseErrorResponse(new ErrorResponse("Internal server error", 5000)));
                })
                .build();
    }

    // ── query binding ─────────────────────────────────────────────────────────

    private List<FixedConstantResponse> turnoverTypes() {
        return TurnoverOperationType.clientTypes.stream().map(ConstantTransformer::enumToFixedConstantResponse).toList();
    }

    private TurnoverSearchRequestDTO bindTurnoverSearch(Map<String, String> params) {
        TurnoverSearchRequestDTO req = new TurnoverSearchRequestDTO();
        applyPaging(params, req);
        req.setFromCreatedAt(dateParam(params, "fromCreatedAt"));
        req.setToCreatedAt(dateParam(params, "toCreatedAt"));
        return req;
    }

    private BridgeTurnoverSearchRequestDTO bindBridgeTurnoverSearch(Map<String, String> params) {
        BridgeTurnoverSearchRequestDTO req = new BridgeTurnoverSearchRequestDTO();
        applyPaging(params, req);
        req.setFromCreatedAt(dateParam(params, "fromCreatedAt"));
        req.setToCreatedAt(dateParam(params, "toCreatedAt"));
        return req;
    }

    private WalletSearchRequestDTO bindWalletSearch(Map<String, String> params) {
        WalletSearchRequestDTO req = new WalletSearchRequestDTO();
        applyPaging(params, req);
        req.setAccountNumbers(longSet(params, "accountNumbers"));
        req.setFromCredit(longParam(params, "fromCredit"));
        req.setToCredit(longParam(params, "toCredit"));
        return req;
    }

    private CreditHistorySearchRequestDTO bindCreditHistorySearch(Map<String, String> params) {
        CreditHistorySearchRequestDTO req = new CreditHistorySearchRequestDTO();
        applyPaging(params, req);
        req.setFromDate(dateParam(params, "fromDate"));
        req.setToDate(dateParam(params, "toDate"));
        req.setCreatedBy(params.get("createdBy"));
        return req;
    }

    private WalletTransactionSearchRequestDTO bindWalletTransactionSearch(Map<String, String> params) {
        WalletTransactionSearchRequestDTO req = new WalletTransactionSearchRequestDTO();
        applyPaging(params, req);
        req.setUserId(params.get("userId"));
        req.setAccountNumber(longParam(params, "dbsAccountNumber"));
        req.setTrackingCode(uuidParam(params, "trackingCode"));
        return req;
    }

    private static void applyPaging(Map<String, String> params, PaginatedRequestDTO req) {
        Integer page = intParam(params, "page");
        Integer size = intParam(params, "size");
        if (page != null) req.setPage(page);
        if (size != null) req.setSize(size);
        String orderBy = params.get("orderByProperty");
        if (orderBy != null) req.setOrderByProperty(orderBy);
    }

    private static Integer intParam(Map<String, String> params, String key) {
        String v = params.get(key);
        return v == null ? null : Integer.valueOf(v);
    }

    private static Long longParam(Map<String, String> params, String key) {
        String v = params.get(key);
        return v == null ? null : Long.valueOf(v);
    }

    private static java.util.Set<Long> longSet(Map<String, String> params, String key) {
        String v = params.get(key);
        if (v == null) return null;
        return Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).collect(Collectors.toSet());
    }

    private static UUID uuidParam(Map<String, String> params, String key) {
        String v = params.get(key);
        return v == null ? null : UUID.fromString(v);
    }

    private static LocalDate dateParam(Map<String, String> params, String key) {
        String v = params.get(key);
        return v == null ? null : LocalDate.parse(v);
    }
}
