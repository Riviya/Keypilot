package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.Exception.ProviderNotFoundException;
import com.rivin.keypilot_gateway.domain.provider.Provider;
import com.rivin.keypilot_gateway.domain.provider.ProviderRegistry;
import com.rivin.keypilot_gateway.domain.proxy.ProxyRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyServiceProviderRoutingTest {

    @Mock private ProviderRegistry providerRegistry;
    @Mock private HttpServletRequest httpServletRequest;
    @Mock private RetryHandler retryHandler;

    private ProxyService proxyService;

    @BeforeEach
    void setUp() {
        proxyService = new ProxyService(providerRegistry, retryHandler);
    }

    // ---------------------------------------------------------------
    // PROVIDER RESOLUTION
    // ---------------------------------------------------------------

    @Test
    void shouldForwardToCorrectProviderBaseUrl() {
        Provider anthropic = new Provider("anthropic", "https://api.anthropic.com");

        when(providerRegistry.resolve("anthropic"))
                .thenReturn(Optional.of(anthropic));
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/messages");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames())
                .thenReturn(Collections.emptyEnumeration());

        when(retryHandler.executeWithRetry(any(), any()))
                .thenReturn(ResponseEntity.ok("{}"));

        proxyService.forward("anthropic", httpServletRequest, "{}");

        ArgumentCaptor<ProxyRequest> captor = ArgumentCaptor.forClass(ProxyRequest.class);

        verify(retryHandler).executeWithRetry(eq("anthropic"), captor.capture());

        ProxyRequest req = captor.getValue();

        assertThat(req.baseUrl()).isEqualTo("https://api.anthropic.com");
        assertThat(req.path()).isEqualTo("/v1/messages");
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

        verify(retryHandler, never()).executeWithRetry(any(), any());
    }

    @Test
    void shouldPassProviderBaseUrlInProxyRequest() {
        Provider openai = new Provider("openai", "https://api.openai.com");

        when(providerRegistry.resolve("openai"))
                .thenReturn(Optional.of(openai));
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames())
                .thenReturn(Collections.emptyEnumeration());

        when(retryHandler.executeWithRetry(any(), any()))
                .thenReturn(ResponseEntity.ok("{}"));

        proxyService.forward("openai", httpServletRequest, "{}");

        ArgumentCaptor<ProxyRequest> captor = ArgumentCaptor.forClass(ProxyRequest.class);

        verify(retryHandler).executeWithRetry(eq("openai"), captor.capture());

        ProxyRequest req = captor.getValue();

        assertThat(req.baseUrl()).isEqualTo("https://api.openai.com");
        assertThat(req.path()).isEqualTo("/v1/chat/completions");
    }
}