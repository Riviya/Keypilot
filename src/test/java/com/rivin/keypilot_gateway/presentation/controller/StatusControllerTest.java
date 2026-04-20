package com.rivin.keypilot_gateway.presentation.controller;


import com.rivin.keypilot_gateway.application.dto.GatewayStatusResponse;
import com.rivin.keypilot_gateway.application.dto.ProviderStatusResponse;
import com.rivin.keypilot_gateway.application.service.GatewayStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatusController.class)
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GatewayStatusService gatewayStatusService;

    @Test
    void shouldReturn200WhenGatewayIsHealthy() throws Exception {
        when(gatewayStatusService.getStatus()).thenReturn(
                new GatewayStatusResponse(
                        true,
                        List.of(new ProviderStatusResponse("openai", true, 2, 2, 0, 0))
                )
        );

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath("$.providers[0].providerName").value("openai"))
                .andExpect(jsonPath("$.providers[0].available").value(true))
                .andExpect(jsonPath("$.providers[0].totalKeys").value(2))
                .andExpect(jsonPath("$.providers[0].availableKeys").value(2));
    }

    @Test
    void shouldReturn503WhenGatewayIsUnhealthy() throws Exception {
        when(gatewayStatusService.getStatus()).thenReturn(
                new GatewayStatusResponse(false, List.of())
        );

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.healthy").value(false));
    }

    @Test
    void shouldReturnAllProviderDetails() throws Exception {
        List<ProviderStatusResponse> providers = List.of(
                new ProviderStatusResponse("openai",    true,  3, 2, 1, 0),
                new ProviderStatusResponse("anthropic", false, 1, 0, 0, 1)
        );
        when(gatewayStatusService.getStatus())
                .thenReturn(new GatewayStatusResponse(true, providers));

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers.length()").value(2))
                .andExpect(jsonPath("$.providers[0].providerName").value("openai"))
                .andExpect(jsonPath("$.providers[0].rateLimitedKeys").value(1))
                .andExpect(jsonPath("$.providers[1].providerName").value("anthropic"))
                .andExpect(jsonPath("$.providers[1].inactiveKeys").value(1));
    }

    @Test
    void shouldNeverExposeApiKeyValuesInResponse() throws Exception {
        when(gatewayStatusService.getStatus()).thenReturn(
                new GatewayStatusResponse(
                        true,
                        List.of(new ProviderStatusResponse("openai", true, 1, 1, 0, 0))
                )
        );

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[0].keyValue").doesNotExist())
                .andExpect(jsonPath("$.providers[0].apiKey").doesNotExist());
    }
}
