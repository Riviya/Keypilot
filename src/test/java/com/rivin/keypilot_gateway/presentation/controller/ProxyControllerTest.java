package com.rivin.keypilot_gateway.presentation.controller;

import com.rivin.keypilot_gateway.application.service.ProxyService;
import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import com.rivin.keypilot_gateway.domain.exception.ProviderCommunicationException;
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
class ProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProxyService proxyService;

    @Test
    void shouldForwardPostRequestAndReturn200() throws Exception {

        // Arrange
        when(proxyService.forward(any(), any(), any()))
                .thenReturn(ResponseEntity.ok("{\"id\":\"chatcmpl-123\"}"));

        // Act and Assert
        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"gpt-4\",\"messages\":[]}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"chatcmpl-123\"}"));
    }

    @Test
    void shouldForwardGetRequestAndReturn200() throws Exception {

        //Arrange
        when(proxyService.forward(any(), any(), any()))
                .thenReturn(ResponseEntity.ok("{\"object\":\"list\"}"));

        // Act and Assert
        mockMvc.perform(get("/v1/models"))
                .andExpect(status().isOk());
    }

    
    @Test
    void shouldReturn503WhenNoApiKeysAvailable() throws Exception {
        when(proxyService.forward(any(), any(), any()))
                .thenThrow(new NoAvailableApiKeyException("No active API keys available"));

        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturn502WhenProviderCallFails() throws Exception {
        when(proxyService.forward(any(), any(), any()))
                .thenThrow(new ProviderCommunicationException("OpenAI unreachable"));

        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadGateway());
    }


}