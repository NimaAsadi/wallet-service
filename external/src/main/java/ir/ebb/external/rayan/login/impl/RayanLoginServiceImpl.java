package ir.ebb.external.rayan.login.impl;

import ir.ebb.external.rayan.login.RayanLoginService;
import ir.ebb.external.rayan.login.dto.request.RayanLoginRequest;
import ir.ebb.external.rayan.login.dto.response.RayanLoginResponse;
import ir.ebb.external.rayan.configuration.RayanHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Replaces the Spring version. Holds the Rayan bearer token with a ttl-based
 * cache. All access is synchronized so concurrent callers can't trigger
 * overlapping logins. Credentials are constructor-injected (from config) instead
 * of {@code @Value}.
 */
@Slf4j
public class RayanLoginServiceImpl implements RayanLoginService {

    private final RayanHttpClient rayanHttpClient;
    private final String username;
    private final String password;
    private final String applicationKey;

    private String token;
    private LocalDateTime tokenExpireTime;

    public RayanLoginServiceImpl(RayanHttpClient rayanHttpClient, String username, String password, String applicationKey) {
        this.rayanHttpClient = rayanHttpClient;
        this.username = username;
        this.password = password;
        this.applicationKey = applicationKey;
    }

    @Override
    public synchronized String getToken() {
        if (token == null || LocalDateTime.now().isAfter(tokenExpireTime)) {
            login();
        }
        return token;
    }

    @Override
    public synchronized String login() {
        log.atInfo().log("Logging in to Rayan gateway");
        RayanLoginResponse response = rayanHttpClient.authenticate(
                new RayanLoginRequest(username, password, applicationKey));
        token = "Bearer " + response.accessToken();
        tokenExpireTime = LocalDateTime.now().plusSeconds(response.ttl() != null ? response.ttl() : 3600);
        log.atInfo().log("Rayan gateway token refreshed, expires at {}", tokenExpireTime);
        return token;
    }
}
