package ir.ebb.external.rayan.login.gateway;

import ir.ebb.external.rayan.login.dto.request.RayanLoginRequest;
import ir.ebb.external.rayan.login.dto.response.RayanLoginResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface RayanLoginGateway {

    @PostExchange("/authenticate")
    RayanLoginResponse getToken(@RequestBody RayanLoginRequest body);
}
