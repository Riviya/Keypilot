package com.rivin.keypilot_gateway.application.service;


import com.rivin.keypilot_gateway.domain.provider.Provider;
import com.rivin.keypilot_gateway.domain.provider.ProviderRegistry;
import com.rivin.keypilot_gateway.application.Exception.ProviderNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

    @Mock private ProviderRegistry providerRegistry;
    @Mock private RetryHandler retryHandler;
    @Mock private HttpServletRequest httpServletRequest;

    private ProxyService proxyService;

    @BeforeEach
    void setUp() {
        proxyService = new ProxyService(providerRegistry, retryHandler);
    }

    @Test
    void shouldDelegateToRetryHandlerWithCorrectProvider() {
        Provider openai = new Provider("openai", "https://api.openai.com");
        when(providerRegistry.resolve("openai")).thenReturn(Optional.of(openai));
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(retryHandler.executeWithRetry(eq("openai"), any()))
                .thenReturn(ResponseEntity.ok("{}"));

        proxyService.forward("openai", httpServletRequest, "{}");

        verify(retryHandler).executeWithRetry(eq("openai"), any());
    }

    @Test
    void shouldThrowWhenProviderNotConfigured() {
        when(providerRegistry.resolve("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                proxyService.forward("unknown", httpServletRequest, "{}")
        ).isInstanceOf(ProviderNotFoundException.class);

        verify(retryHandler, never()).executeWithRetry(any(), any());
    }

    @Test
    void shouldStripAuthorizationHeaderBeforeForwarding() {
        Provider openai = new Provider("openai", "https://api.openai.com");
        when(providerRegistry.resolve("openai")).thenReturn(Optional.of(openai));
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(
                        java.util.List.of("Authorization", "Content-Type")
                ));
        when(httpServletRequest.getHeader("Content-Type")).thenReturn("application/json");
        when(retryHandler.executeWithRetry(any(), any()))
                .thenReturn(ResponseEntity.ok("{}"));

        proxyService.forward("openai", httpServletRequest, "{}");

        verify(retryHandler).executeWithRetry(any(), argThat(req ->
                !req.headers().containsKey("Authorization")
        ));
    }
}