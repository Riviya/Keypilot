package com.rivin.keypilot_gateway.integration;


import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class RetryIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        // Reset WireMock (Base class already starts it)
        wireMockServer.resetAll();

        // Seed keys
        apiKeyRepository.save(new ApiKey("sk-key-one", "openai", 100, 60));
        apiKeyRepository.save(new ApiKey("sk-key-two", "openai", 100, 60));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
        apiKeyRepository.findAll().forEach(k -> apiKeyRepository.delete(k.getId()));
    }

    // ---------------------------------------------------------------
    // RETRY ON 429
    // ---------------------------------------------------------------

    @Test
    void shouldRetryWithSecondKeyWhenFirstKeyReturns429() throws Exception {
        // First request → 429
        // Second request → 200
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .inScenario("rate-limit-retry")
                        .whenScenarioStateIs("Started")
                        .willReturn(aResponse().withStatus(429).withBody("rate limited"))
                        .willSetStateTo("first-attempt-done")
        );

        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .inScenario("rate-limit-retry")
                        .whenScenarioStateIs("first-attempt-done")
                        .willReturn(okJson("""
                    {"id":"retry-success","choices":[{"message":{"content":"Hi!"}}]}
                """))
        );

        // Caller sends ONE request
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                // Caller receives 200 — never sees the 429
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("retry-success"));

        // Gateway made exactly 2 calls to the provider
        wireMockServer.verify(2,
                postRequestedFor(urlEqualTo("/v1/chat/completions"))
        );
    }

    @Test
    void shouldReturn503WhenAllKeysExhaustedAfterRetries() throws Exception {
        // Every attempt returns 429
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .willReturn(aResponse().withStatus(429).withBody("all limited"))
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.provider").value("openai"));
    }

    @Test
    void shouldNotRetryOn500ProviderError() throws Exception {
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .willReturn(aResponse().withStatus(500).withBody("server error"))
        );

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is5xxServerError());

        // Only 1 call — no retry on 500
        wireMockServer.verify(1,
                postRequestedFor(urlEqualTo("/v1/chat/completions"))
        );
    }
}