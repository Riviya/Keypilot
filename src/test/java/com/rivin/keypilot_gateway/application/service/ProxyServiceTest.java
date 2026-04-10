package com.rivin.keypilot_gateway.application.service;

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

    private ProxyService proxyService;

    @BeforeEach
    void setUp() {
        proxyService = new ProxyService(keyRotationService, providerGateway);
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
//
//    @Test
//    void shouldNotForwardAuthorizationHeaderFromIncomingRequest() {
//        // Security: caller's own Authorization header must be stripped
//        // and replaced with the gateway-selected key
//        ApiKey selectedKey = new ApiKey("sk-gateway-key", "openai");
//        when(keyRotationService.getNextKey("openai")).thenReturn(selectedKey);
//        when(httpServletRequest.getRequestURI()).thenReturn("/v1/chat/completions");
//        when(httpServletRequest.getMethod()).thenReturn("POST");
//
//        // Simulate incoming request that has its own Authorization header
//        when(httpServletRequest.getHeaderNames())
//                .thenReturn(Collections.enumeration(java.util.List.of("Authorization", "Content-Type")));
//        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer sk-caller-own-key");
//        when(httpServletRequest.getHeader("Content-Type")).thenReturn("application/json");
//
//        when(providerGateway.forward(any())).thenReturn(ResponseEntity.ok("{}"));
//
//        proxyService.forward("openai", httpServletRequest, "{}");
//
//        verify(providerGateway).forward(argThat(proxyRequest ->
//                proxyRequest.apiKey().equals("sk-gateway-key") &&
//                        !proxyRequest.headers().containsKey("Authorization")
//        ));
//    }

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

}