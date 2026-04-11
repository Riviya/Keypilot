package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.domain.exception.ProviderCommunicationException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.proxy.ProviderGateway;
import com.rivin.keypilot_gateway.domain.proxy.ProxyRequest;
import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

    @Mock
    private KeyRotationService keyRotationService;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private RateLimiterService rateLimiterService;

    private ProxyService proxyService;

    @BeforeEach
    void setUp() {
        proxyService = new ProxyService(keyRotationService, providerGateway, rateLimiterService);
    }

    // ---------------------------------------------------------------
    // HAPPY PATH
    // ---------------------------------------------------------------

    @Test
    void shouldInjectSelectedApiKeyIntoForwardedRequest() {
        // Arrange
        ApiKey selectedKey = new ApiKey("sk-secret-key", "openai");
        when(keyRotationService.getNextKey("openai")).thenReturn(selectedKey);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(providerGateway.forward(any(ProxyRequest.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        // Act
        proxyService.forward("openai", httpServletRequest, "{}");

        // Assert — verify the key was injected into the forwarded request
        verify(providerGateway).forward(argThat(proxyRequest ->
                proxyRequest.apiKey().equals("sk-secret-key")
        ));
    }

    @Test
    void shouldForwardCorrectPathToProvider() {

        // Arrange
        ApiKey selectedKey = new ApiKey("sk-test", "openai");
        when(keyRotationService.getNextKey("openai")).thenReturn(selectedKey);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/models");
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(providerGateway.forward(any(ProxyRequest.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        // Act
        proxyService.forward("openai", httpServletRequest, null);

        // Assert
        verify(providerGateway).forward(argThat(proxyRequest ->
                proxyRequest.path().equals("/v1/models")
        ));
    }

    @Test
    void shouldReturnResponseFromProvider() {

        // Arrange
        ApiKey selectedKey = new ApiKey("sk-test", "openai");
        when(keyRotationService.getNextKey("openai")).thenReturn(selectedKey);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        ResponseEntity<String> providerResponse = ResponseEntity.ok("{\"result\":\"ok\"}");
        when(providerGateway.forward(any(ProxyRequest.class))).thenReturn(providerResponse);

        ResponseEntity<String> result = proxyService.forward("openai", httpServletRequest, "{}");

        // Assert
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo("{\"result\":\"ok\"}");
    }

    @Test
    void shouldPreserveHttpMethodInForwardedRequest() {

        // Arrange
        ApiKey selectedKey = new ApiKey("openai", "sk-test");
        when(keyRotationService.getNextKey("openai")).thenReturn(selectedKey);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(providerGateway.forward(any())).thenReturn(ResponseEntity.ok("{}"));

        // Act
        proxyService.forward("openai", httpServletRequest, "{}");

        // Assert
        verify(providerGateway).forward(argThat(proxyRequest ->
                proxyRequest.method().equals(HttpMethod.POST)
        ));
    }

    // ---------------------------------------------------------------
    // FAILURE CASES
    // ---------------------------------------------------------------

    @Test
    void shouldPropagateNoAvailableApiKeyException() {

        // Arrange
        when(keyRotationService.getNextKey("openai"))
                .thenThrow(new NoAvailableApiKeyException("No active API keys available"));

        // Act and Assert
        assertThatThrownBy(() ->
                proxyService.forward("openai", httpServletRequest, "{}"))
                .isInstanceOf(NoAvailableApiKeyException.class);

        // Gateway must never be called
        verify(providerGateway, never()).forward(any());
    }

    @Test
    void shouldNotForwardAuthorizationHeaderFromIncomingRequest() {

        // Arrange
        ApiKey selectedKey = new ApiKey("sk-gateway-key", "openai");
        when(keyRotationService.getNextKey("openai"))
                .thenReturn(selectedKey);
        when(httpServletRequest.getRequestURI())
                .thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod())
                .thenReturn("POST");
        when(httpServletRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(List.of("Authorization", "Content-Type")));
        when(httpServletRequest.getHeader("Content-Type"))
                .thenReturn("application/json");

        // Act
        proxyService.forward("openai", httpServletRequest, "{}");

        // Assert
        verify(providerGateway).forward(argThat(request ->
                request.apiKey().equals("sk-gateway-key") &&
                        !request.headers().containsKey("Authorization") &&
                        request.headers().get("Content-Type").equals("application/json")
        ));
    }

    ////////////////////////////////// Usage Recording Check //////////////////////////////////////
    @Test
    void shouldRecordKeyUsageAfterSuccessfulForward() {

        // Arrange
        ApiKey selectedKey = new ApiKey("sk-test", "openai", 10, 40);

        when(keyRotationService.getNextKey("openai")).thenReturn(selectedKey);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(providerGateway.forward(any(ProxyRequest.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        proxyService.forward("openai", httpServletRequest, "{}");
        verify(rateLimiterService, times(1)).recordUsage(selectedKey.getId());

    }

    @Test
    void shouldNotRecordUsageWhenProviderCallFails() {
        ApiKey selectedKey = new ApiKey("sk-test", "openai", 10, 60);
        when(keyRotationService.getNextKey("openai")).thenReturn(selectedKey);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(providerGateway.forward(any()))
                .thenThrow(new ProviderCommunicationException("OpenAI down"));

        assertThatThrownBy(() -> proxyService.forward("openai", httpServletRequest, "{}"))
                .isInstanceOf(ProviderCommunicationException.class);

        verify(rateLimiterService, never()).recordUsage(any());
    }

    @Test
    void shouldRecordUsageWhenProviderReturnsErrorResponse() {
        // Arrange
        ApiKey selectedKey = new ApiKey("sk-test", "openai", 10, 60);

        when(keyRotationService.getNextKey("openai")).thenReturn(selectedKey);
        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(500).body("error"));

        // Act
        proxyService.forward("openai", httpServletRequest, "{}");

        // Should STILL record usage
        verify(rateLimiterService).recordUsage(selectedKey.getId());
    }

}


