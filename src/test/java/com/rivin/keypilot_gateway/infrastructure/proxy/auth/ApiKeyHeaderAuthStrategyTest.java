package com.rivin.keypilot_gateway.infrastructure.proxy.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ApiKeyHeaderAuthStrategyTest {

    @Test
    void shouldAddApiKeyWithConfiguredHeaderName() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Cohere uses "Authorization: Bearer" but some providers use "X-Api-Key"
        server.expect(requestTo("https://api.cohere.com/v1/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "cohere-key-123"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RestClient restClient = builder.build();
        ApiKeyHeaderAuthStrategy strategy = new ApiKeyHeaderAuthStrategy("x-api-key");

        restClient.post()
                .uri("https://api.cohere.com/v1/generate")
                .headers(headers -> strategy.apply(headers, "cohere-key-123"))
                .retrieve()
                .toBodilessEntity();

        server.verify();
    }

    @Test
    void shouldNotAddBearerPrefixForHeaderKeyStrategy() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Raw key value — no "Bearer " prefix
        server.expect(requestTo("https://api.example.com/endpoint"))
                .andExpect(header("x-api-key", "raw-key-value"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RestClient restClient = builder.build();
        ApiKeyHeaderAuthStrategy strategy = new ApiKeyHeaderAuthStrategy("x-api-key");

        restClient.post()
                .uri("https://api.example.com/endpoint")
                .headers(headers -> strategy.apply(headers, "raw-key-value"))
                .retrieve()
                .toBodilessEntity();

        server.verify();
    }
}
