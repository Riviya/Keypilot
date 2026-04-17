package com.rivin.keypilot_gateway.infrastructure.proxy.auth;


import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BearerAuthStrategyTest {

    // ---------------------------------------------------------------
    // We test strategy behaviour by verifying the Authorization header
    // appears correctly on the outbound request
    // ---------------------------------------------------------------

    @Test
    void shouldAddBearerTokenAuthorizationHeader() {
        // Arrange
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test-key"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RestClient restClient = builder.build();
        BearerAuthStrategy strategy = new BearerAuthStrategy();

        // Act — apply the strategy to the request spec
        restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .headers(headers -> strategy.apply(headers, "sk-test-key"))
                .retrieve()
                .toBodilessEntity();

        // Assert — MockRestServiceServer verifies the expectation
        server.verify();
    }

    @Test
    void shouldFormatHeaderAsBearerPrefixPlusKey() {
        // Different key value — prefix must always be "Bearer "
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(header("Authorization", "Bearer ant-key-xyz"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RestClient restClient = builder.build();
        BearerAuthStrategy strategy = new BearerAuthStrategy();

        restClient.post()
                .uri("https://api.anthropic.com/v1/messages")
                .headers(headers -> strategy.apply(headers, "ant-key-xyz"))
                .retrieve()
                .toBodilessEntity();

        server.verify();
    }

    @Test
    void shouldNotAddQueryParametersForBearerAuth() {
        // Bearer auth must only use headers — no URL contamination
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Instead of testing what's NOT there, verify what IS there
        server.expect(requestTo("https://api.openai.com/v1/models"))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RestClient restClient = builder.build();
        BearerAuthStrategy strategy = new BearerAuthStrategy();

        restClient.get()
                .uri("https://api.openai.com/v1/models")
                .headers(headers -> strategy.apply(headers, "sk-test"))
                .retrieve()
                .toBodilessEntity();

        server.verify();
    }
}