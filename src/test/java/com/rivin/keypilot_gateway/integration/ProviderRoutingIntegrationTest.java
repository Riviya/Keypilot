package com.rivin.keypilot_gateway.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProviderRoutingIntegrationTest {

    // One WireMock server simulates both providers
    // We distinguish them by the path and auth header
    static WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort()
    );

    static { wireMockServer.start(); }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        String baseUrl = wireMockServer.baseUrl();
        registry.add("gateway.providers.openai.base-url",    () -> baseUrl);
        registry.add("gateway.providers.anthropic.base-url", () -> baseUrl);
        registry.add("gateway.storage.path",
                () -> System.getProperty("java.io.tmpdir") + "/gateway-routing-test.json");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        apiKeyRepository.save(new ApiKey("sk-openai-key",    "openai",    100, 60));
        apiKeyRepository.save(new ApiKey("sk-anthropic-key", "anthropic", 100, 60));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
        apiKeyRepository.findAll().forEach(k -> apiKeyRepository.delete(k.getId()));
    }

    @Test
    void shouldRouteToOpenAiWithCorrectKey() throws Exception {
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .withHeader("Authorization", equalTo("Bearer sk-openai-key"))
                        .willReturn(okJson("""
                    {"provider":"openai-response"}
                """))
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("openai-response"));
    }

    @Test
    void shouldRouteToAnthropicWithCorrectKey() throws Exception {
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/messages"))
                        .withHeader("Authorization", equalTo("Bearer sk-anthropic-key"))
                        .willReturn(okJson("""
                    {"provider":"anthropic-response"}
                """))
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/messages")
                        .header("X-Gateway-Provider", "anthropic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("anthropic-response"));
    }

    @Test
    void shouldHandleCaseInsensitiveProviderHeader() throws Exception {
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .willReturn(okJson("{}"))
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "OpenAI")   // mixed case
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------
    // STATUS ENDPOINT — integration level
    // ---------------------------------------------------------------

    @Test
    void shouldReturnHealthyStatusWhenKeysAvailable() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath(
                        "$.providers[?(@.providerName == 'openai')].available"
                ).value(true));
    }

    @Test
    void shouldNeverExposeApiKeyValuesInStatusResponse() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/status"))
                .andExpect(status().isOk())
                // These field names must not exist anywhere in the response
                .andExpect(jsonPath("$..keyValue").doesNotExist())
                .andExpect(jsonPath("$..apiKey").doesNotExist());
    }

    @Test
    void shouldIncludeCorrelationIdInEveryResponse() throws Exception {
        wireMockServer.stubFor(
                post(anyUrl()).willReturn(okJson("{}"))
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void shouldUseCallerProvidedCorrelationId() throws Exception {
        wireMockServer.stubFor(
                post(anyUrl()).willReturn(okJson("{}"))
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "openai")
                        .header("X-Correlation-ID", "my-trace-id-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "my-trace-id-123"));
    }
}