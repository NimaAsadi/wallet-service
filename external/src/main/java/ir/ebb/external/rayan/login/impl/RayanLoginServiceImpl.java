package ir.ebb.external.rayan.login.impl;

import ir.ebb.external.rayan.login.RayanLoginService;
import ir.ebb.external.rayan.login.dto.request.RayanLoginRequest;
import ir.ebb.external.rayan.login.dto.response.RayanLoginResponse;
import ir.ebb.external.rayan.login.gateway.RayanLoginGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RayanLoginServiceImpl implements RayanLoginService {

    private final RayanLoginGateway rayanLoginGateway;

    @Value("${rayan.auth.username}")
    private String username;

    @Value("${rayan.auth.password}")
    private String password;

    @Value("${rayan.auth.application-key}")
    private String applicationKey;

    private String token;
    private LocalDateTime tokenExpireTime;

    @Override
    public String getToken() {
        if (token == null || LocalDateTime.now().isAfter(tokenExpireTime)) {
            login();
        }
        return token;
    }

    @Override
    public String login() {
        log.atInfo().log("Logging in to Rayan gateway");
        RayanLoginResponse response = rayanLoginGateway.getToken(
                new RayanLoginRequest(username, password, applicationKey));
        token = "Bearer " + response.accessToken();
        tokenExpireTime = LocalDateTime.now().plusSeconds(response.ttl() != null ? response.ttl() : 3600);
        log.atInfo().log("Rayan gateway token refreshed, expires at {}", tokenExpireTime);
        return token;
    }
}
