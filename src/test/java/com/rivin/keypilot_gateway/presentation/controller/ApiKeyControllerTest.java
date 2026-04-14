package com.rivin.keypilot_gateway.presentation.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivin.keypilot_gateway.application.dto.AddApiKeyRequest;
import com.rivin.keypilot_gateway.application.dto.ApiKeyResponse;
import com.rivin.keypilot_gateway.application.service.ApiKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiKeyController.class)
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApiKeyService apiKeyService;


    ////////////////////////////////// POST /api/keys Check //////////////////////////////////////

    @Test
    void shouldReturn201WhenApiKeyAdded() throws Exception {
        // Arrange
        AddApiKeyRequest request = new AddApiKeyRequest("sk-test-123", "openai");
        ApiKeyResponse response = new ApiKeyResponse("uuid-1", "openai", true);
        when(apiKeyService.addKey(any(AddApiKeyRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("uuid-1"))
                .andExpect(jsonPath("$.provider").value("openai"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn400WhenProviderIsMissing() throws Exception {
        // Arrange - provider is null
        String badBody = """
            { "keyValue": "sk-test-123" }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenKeyValueIsMissing() throws Exception {
        String badBody = """
            { "provider": "openai" }
            """;

        mockMvc.perform(post("/api/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest());
    }


    ////////////////////////////////// GET /api/keys Check //////////////////////////////////////
    @Test
    void shouldReturn200WithListOfKeys() throws Exception {
        // Arrange
        List<ApiKeyResponse> responses = List.of(
                new ApiKeyResponse("uuid-1", "openai", true),
                new ApiKeyResponse("uuid-2", "anthropic", true)
        );
        when(apiKeyService.listAllKeys()).thenReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].provider").value("openai"))
                .andExpect(jsonPath("$[1].provider").value("anthropic"));
    }

    @Test
    void shouldReturn200WithEmptyListWhenNoKeys() throws Exception {
        when(apiKeyService.listAllKeys()).thenReturn(List.of());

        mockMvc.perform(get("/api/keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }


    ////////////////////////////////// DELETE /api/keys/{id} Check //////////////////////////////////////
    @Test
    void shouldReturn204WhenKeyDeleted() throws Exception {
        doNothing().when(apiKeyService).deleteKey("uuid-1");

        mockMvc.perform(delete("/api/keys/uuid-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentKey() throws Exception {
        doThrow(new IllegalArgumentException("No API key found with id: ghost-id"))
                .when(apiKeyService).deleteKey("ghost-id");

        mockMvc.perform(delete("/api/keys/ghost-id"))
                .andExpect(status().isNotFound());
    }
}