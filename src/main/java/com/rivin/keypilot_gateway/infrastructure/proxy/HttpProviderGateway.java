package com.rivin.keypilot_gateway.infrastructure.proxy;


import com.rivin.keypilot_gateway.domain.exception.ProviderCommunicationException;
import com.rivin.keypilot_gateway.domain.proxy.ProviderGateway;
import com.rivin.keypilot_gateway.domain.proxy.ProxyRequest;
import com.rivin.keypilot_gateway.infrastructure.proxy.auth.ProviderAuthStrategy;
import com.rivin.keypilot_gateway.infrastructure.proxy.auth.ProviderAuthStrategyFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpProviderGateway implements ProviderGateway {

    private final RestClient restClient;
    private final ProviderAuthStrategyFactory authStrategyFactory;

    public HttpProviderGateway(ProviderAuthStrategyFactory authStrategyFactory) {
        this.restClient = RestClient.builder().build();
        this.authStrategyFactory = authStrategyFactory;
    }

//    // Package-private for testing
//    HttpProviderGateway(RestClient restClient,
//                        ProviderAuthStrategyFactory authStrategyFactory) {
//        this.restClient = restClient;
//        this.authStrategyFactory = authStrategyFactory;
//    }

    @Override
    public ResponseEntity<String> forward(ProxyRequest request) {
        ProviderAuthStrategy authStrategy =
                authStrategyFactory.getStrategy(request.provider());

        // Auth strategy may modify the URL (QueryParam) or leave it unchanged
        String targetUrl = authStrategy.buildAuthenticatedUrl(
                buildTargetUrl(request.baseUrl(), request.path()),
                request.apiKey()
        );

        return restClient.method(request.method())
                .uri(targetUrl)
                .headers(headers -> {
                    authStrategy.apply(headers, request.apiKey());
                    request.headers().forEach(headers::add);
                    headers.set("Content-Type", "application/json");
                })
                .body(request.body() != null ? request.body() : "")
                .exchange((req, res) -> {
                    try {
                        String body = res.bodyTo(String.class);
                        return ResponseEntity
                                .status(res.getStatusCode())
                                .body(body);
                    } catch (Exception e) {
                        throw new ProviderCommunicationException(
                                "Failed to read provider response", e
                        );
                    }
                });
    }

    String buildTargetUrl(String baseUrl, String path) {
        String cleanBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return cleanBase + cleanPath;
    }
}