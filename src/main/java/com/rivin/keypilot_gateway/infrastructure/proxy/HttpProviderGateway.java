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

        try {
            return restClient.method(request.method())
                    .uri(targetUrl)
                    .headers(headers -> {
                        // Auth strategy may set headers (Bearer, ApiKeyHeader)
                        // or be a no-op (QueryParam)
                        authStrategy.apply(headers, request.apiKey());

                        // Forward caller's headers (already stripped of Authorization)
                        request.headers().forEach(headers::add);

                        headers.set("Content-Type", "application/json");
                    })
                    .body(request.body() != null ? request.body() : "")
                    .retrieve()
                    .toEntity(String.class);

        } catch (RestClientException ex) {
            throw new ProviderCommunicationException(
                    "Failed to reach provider [" + request.provider() + "]: "
                            + ex.getMessage(), ex
            );
        }
    }

    String buildTargetUrl(String baseUrl, String path) {
        String cleanBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return cleanBase + cleanPath;
    }
}