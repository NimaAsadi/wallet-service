package ir.ebb.external.rayan.configuration;

import ir.ebb.external.rayan.login.gateway.RayanLoginGateway;
import ir.ebb.external.rayan.wallet.gateway.RayanWalletGateway;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Slf4j
@Configuration
public class RayanConfig {

    public static final String HEADER_NAME_TOKEN = "Authorization";

    @Value("${external.rayan-gateway.base-url}")
    private String baseUrl;

    @Bean
    public RestClient rayanRestClient() {
        return buildRestClient(baseUrl);
    }

    @Bean
    public RayanWalletGateway rayanWalletGateway(RestClient rayanRestClient) {
        RestClientAdapter adapter = RestClientAdapter.create(rayanRestClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(RayanWalletGateway.class);
    }

    @Bean
    public RayanLoginGateway rayanLoginGateway(RestClient rayanRestClient) {
        RestClientAdapter adapter = RestClientAdapter.create(rayanRestClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(RayanLoginGateway.class);
    }

    @NotNull
    private RestClient buildRestClient(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(getClientHttpRequestFactory())
                .requestInterceptor((request, body, execution) -> {
                    log.atInfo().log("{}: {}", request.getMethod(), request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(0);
        factory.setConnectionRequestTimeout(0);
        return factory;
    }
}
