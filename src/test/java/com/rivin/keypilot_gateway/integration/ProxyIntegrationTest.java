package com.rivin.keypilot_gateway.integration;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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


class ProxyIntegrationTest extends BaseIntegrationTest {

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
    // FULL PROXY FLOW
    // ---------------------------------------------------------------

    @Test
    void shouldForwardRequestToProviderAndReturnResponse() throws Exception {
        // Arrange — WireMock returns a valid OpenAI-style response
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .willReturn(okJson("""
                    {
                      "id": "chatcmpl-integration-test",
                      "object": "chat.completion",
                      "choices": [{"message": {"content": "Hello!"}}]
                    }
                """))
        );

        // Act — call the gateway as a developer's app would
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"model":"gpt-4o","messages":[{"role":"user","content":"Hi"}]}
                """))
                // Assert — gateway returns provider's response transparently
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("chatcmpl-integration-test"))
                .andExpect(jsonPath("$.choices[0].message.content").value("Hello!"));

        // Verify the gateway actually hit WireMock (not a cached/mocked response)
        wireMockServer.verify(1,
                postRequestedFor(urlEqualTo("/v1/chat/completions"))
        );
    }

    @Test
    void shouldInjectApiKeyIntoAuthorizationHeader() throws Exception {
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .withHeader("Authorization", equalTo("Bearer sk-key-one"))
                        .willReturn(okJson("{}"))
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // WireMock will only match if Authorization header is correct
        // If it doesn't match, WireMock returns 404 and the test fails
        wireMockServer.verify(1,
                postRequestedFor(urlEqualTo("/v1/chat/completions"))
                        .withHeader("Authorization", equalTo("Bearer sk-key-one"))
        );
    }

    @Test
    void shouldNotForwardGatewayRoutingHeaderToProvider() throws Exception {

        // Arrange: provider does NOT care about gateway header
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .willReturn(okJson("{}"))
        );

        // Act
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/v1/chat/completions")
                                .header("X-Gateway-Provider", "openai")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk());

        // Assert (THIS is where header filtering is validated)
        wireMockServer.verify(1,
                postRequestedFor(urlEqualTo("/v1/chat/completions"))
                        .withoutHeader("X-Gateway-Provider")
        );
    }

    @Test
    void shouldReturn404WhenProviderNotConfigured() throws Exception {
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "nonexistent-ai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn503WhenNoKeysConfiguredForProvider() throws Exception {
        // No keys seeded for anthropic
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/messages")
                        .header("X-Gateway-Provider", "anthropic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldUseDefaultProviderWhenNoHeaderProvided() throws Exception {
        wireMockServer.stubFor(
                post(urlEqualTo("/v1/chat/completions"))
                        .willReturn(okJson("{}"))
        );

        // No X-Gateway-Provider header — should default to openai
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        wireMockServer.verify(1,
                postRequestedFor(urlEqualTo("/v1/chat/completions"))
        );
    }

    @Test
    void shouldForwardGetRequestsCorrectly() throws Exception {
        wireMockServer.stubFor(
                get(urlEqualTo("/v1/models"))
                        .willReturn(okJson("""
                    {"object":"list","data":[{"id":"gpt-4o"}]}
                """))
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v1/models")
                        .header("X-Gateway-Provider", "openai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("gpt-4o"));
    }
}