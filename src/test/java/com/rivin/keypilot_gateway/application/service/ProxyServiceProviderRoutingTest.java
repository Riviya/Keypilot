package com.rivin.keypilot_gateway.application.service;


import com.rivin.keypilot_gateway.application.Exception.ProviderNotFoundException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.provider.Provider;
import com.rivin.keypilot_gateway.domain.provider.ProviderRegistry;
import com.rivin.keypilot_gateway.domain.proxy.ProviderGateway;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyServiceProviderRoutingTest {

    @Mock private KeyRotationService keyRotationService;
    @Mock private ProviderGateway providerGateway;
    @Mock private RateLimiterService rateLimiterService;
    @Mock private ProviderRegistry providerRegistry;
    @Mock private HttpServletRequest httpServletRequest;

    private ProxyService proxyService;

    @BeforeEach
    void setUp() {
        proxyService = new ProxyService(
                keyRotationService,
                providerGateway,
                rateLimiterService,
                providerRegistry
        );
    }

    // ---------------------------------------------------------------
    // PROVIDER RESOLUTION
    // ---------------------------------------------------------------

    @Test
    void shouldForwardToCorrectProviderBaseUrl() {
        Provider anthropic = new Provider("anthropic", "https://api.anthropic.com");
        ApiKey key = new ApiKey("sk-ant-test", "anthropic");

        when(providerRegistry.resolve("anthropic"))
                .thenReturn(Optional.of(anthropic));
        when(keyRotationService.getNextKey("anthropic")).thenReturn(key);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/messages");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames())
                .thenReturn(Collections.emptyEnumeration());
        when(providerGateway.forward(any())).thenReturn(ResponseEntity.ok("{}"));

        proxyService.forward("anthropic", httpServletRequest, "{}");

        verify(providerGateway).forward(argThat(req ->
                req.baseUrl().equals("https://api.anthropic.com")
        ));
    }

    @Test
    void shouldThrowWhenProviderNotConfigured() {
        when(providerRegistry.resolve("unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                proxyService.forward("unknown", httpServletRequest, "{}")
        )
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining("unknown");

        verify(providerGateway, never()).forward(any());
    }

    @Test
    void shouldPassProviderBaseUrlInProxyRequest() {
        Provider openai = new Provider("openai", "https://api.openai.com");
        ApiKey key = new ApiKey("sk-test", "openai");

        when(providerRegistry.resolve("openai")).thenReturn(Optional.of(openai));
        when(keyRotationService.getNextKey("openai")).thenReturn(key);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames())
                .thenReturn(Collections.emptyEnumeration());
        when(providerGateway.forward(any())).thenReturn(ResponseEntity.ok("{}"));

        proxyService.forward("openai", httpServletRequest, "{}");

        verify(providerGateway).forward(argThat(req ->
                req.baseUrl().equals("https://api.openai.com") &&
                        req.path().equals("/v1/chat/completions")
        ));
    }
}