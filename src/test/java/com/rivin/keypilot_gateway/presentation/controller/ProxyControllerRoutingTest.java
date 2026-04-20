package com.rivin.keypilot_gateway.presentation.controller;

import com.rivin.keypilot_gateway.application.service.ProxyService;
import com.rivin.keypilot_gateway.application.Exception.ProviderNotFoundException;
import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProxyController.class)
class
ProxyControllerRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProxyService proxyService;

    // ---------------------------------------------------------------
    // PROVIDER HEADER ROUTING
    // ---------------------------------------------------------------

    @Test
    void shouldUseProviderFromHeader() throws Exception {
        when(proxyService.forward(eq("anthropic"), any(), any()))
                .thenReturn(ResponseEntity.ok("{}"));

        mockMvc.perform(post("/v1/messages")
                        .header("X-Gateway-Provider", "anthropic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(proxyService).forward(eq("anthropic"), any(), any());
    }

    @Test
    void shouldFallBackToDefaultProviderWhenNoHeaderPresent() throws Exception {
        when(proxyService.forward(eq("openai"), any(), any()))
                .thenReturn(ResponseEntity.ok("{}"));

        // No X-Gateway-Provider header
        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(proxyService).forward(eq("openai"), any(), any());
    }

    @Test
    void shouldReturn404WhenProviderNotConfigured() throws Exception {
        when(proxyService.forward(eq("unknown"), any(), any()))
                .thenThrow(new ProviderNotFoundException("unknown"));

        mockMvc.perform(post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn503WhenNoKeysAvailableForProvider() throws Exception {
        when(proxyService.forward(any(), any(), any()))
                .thenThrow(new NoAvailableApiKeyException("No active keys"));

        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldHandleCaseInsensitiveProviderHeader() throws Exception {
        when(proxyService.forward(eq("openai"), any(), any()))
                .thenReturn(ResponseEntity.ok("{}"));

        mockMvc.perform(post("/v1/chat/completions")
                        .header("X-Gateway-Provider", "OpenAI")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(proxyService).forward(eq("openai"), any(), any());
    }
}