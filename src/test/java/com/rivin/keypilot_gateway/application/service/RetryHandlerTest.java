package com.rivin.keypilot_gateway.application.service;


import com.rivin.keypilot_gateway.application.Exception.RetryExhaustedException;
import com.rivin.keypilot_gateway.application.retry.RetryPolicy;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.exception.ProviderCommunicationException;
import com.rivin.keypilot_gateway.domain.proxy.ProviderGateway;
import com.rivin.keypilot_gateway.domain.proxy.ProxyRequest;
import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryHandlerTest {

    @Mock private KeyRotationService keyRotationService;
    @Mock private RateLimiterService rateLimiterService;
    @Mock private ProviderGateway providerGateway;

    private RetryHandler retryHandler;

    private static final ProxyRequest BASE_REQUEST = new ProxyRequest(
            "openai",
            "https://api.openai.com",
            "/v1/chat/completions",
            HttpMethod.POST,
            "sk-placeholder",       // will be replaced per attempt
            Map.of(),
            "{}"
    );

    @BeforeEach
    void setUp() {
        retryHandler = new RetryHandler(
                keyRotationService,
                rateLimiterService,
                providerGateway,
                new RetryPolicy(3)
        );
    }

    // ---------------------------------------------------------------
    // SUCCESS ON FIRST ATTEMPT
    // ---------------------------------------------------------------

    @Test
    void shouldReturnResponseWhenFirstAttemptSucceeds() {
        ApiKey key = new ApiKey("sk-good", "openai");
        when(keyRotationService.getNextKey("openai")).thenReturn(key);
        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.ok("{\"result\":\"ok\"}"));

        ResponseEntity<String> result = retryHandler.executeWithRetry(
                "openai", BASE_REQUEST
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo("{\"result\":\"ok\"}");
        verify(providerGateway, times(1)).forward(any());
    }

    // ---------------------------------------------------------------
    // RETRY ON 429
    // ---------------------------------------------------------------

    @Test
    void shouldRetryWithDifferentKeyWhenProviderReturns429() {
        ApiKey firstKey  = new ApiKey("sk-limited", "openai");
        ApiKey secondKey = new ApiKey("sk-good", "openai");

        when(keyRotationService.getNextKey("openai"))
                .thenReturn(firstKey)
                .thenReturn(secondKey);

        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(429).body("rate limited"))
                .thenReturn(ResponseEntity.ok("{}"));

        ResponseEntity<String> result = retryHandler.executeWithRetry(
                "openai", BASE_REQUEST
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(providerGateway, times(2)).forward(any());
    }

    @Test
    void shouldMarkKeyAsRateLimitedAfter429() {
        ApiKey firstKey  = new ApiKey("sk-limited", "openai");
        ApiKey secondKey = new ApiKey("sk-good", "openai");

        when(keyRotationService.getNextKey("openai"))
                .thenReturn(firstKey)
                .thenReturn(secondKey);

        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(429).body("rate limited"))
                .thenReturn(ResponseEntity.ok("{}"));

        retryHandler.executeWithRetry("openai", BASE_REQUEST);

        // The rate-limited key must be recorded as exhausted
        verify(rateLimiterService).markAsRateLimited(firstKey.getId());
        // The successful key must be recorded as used normally
        verify(rateLimiterService).recordUsage(secondKey.getId());
    }

    @Test
    void shouldUseCorrectKeyValueInEachRetryAttempt() {
        ApiKey firstKey  = new ApiKey("sk-first", "openai");
        ApiKey secondKey = new ApiKey("sk-second", "openai");

        when(keyRotationService.getNextKey("openai"))
                .thenReturn(firstKey)
                .thenReturn(secondKey);

        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(429).body("rate limited"))
                .thenReturn(ResponseEntity.ok("{}"));

        retryHandler.executeWithRetry("openai", BASE_REQUEST);

        // First attempt used first key
        verify(providerGateway).forward(argThat(req ->
                req.apiKey().equals("sk-first")
        ));
        // Second attempt used second key
        verify(providerGateway).forward(argThat(req ->
                req.apiKey().equals("sk-second")
        ));
    }

    @Test
    void shouldNotRetryOn500Error() {
        ApiKey key = new ApiKey("sk-test", "openai");
        when(keyRotationService.getNextKey("openai")).thenReturn(key);
        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(500).body("server error"));

        ResponseEntity<String> result = retryHandler.executeWithRetry(
                "openai", BASE_REQUEST
        );

        // 500 passes through — no retry
        assertThat(result.getStatusCode().value()).isEqualTo(500);
        verify(providerGateway, times(1)).forward(any());
    }

    @Test
    void shouldNotRetryOn400Error() {
        ApiKey key = new ApiKey("sk-test", "openai");
        when(keyRotationService.getNextKey("openai")).thenReturn(key);
        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(400).body("bad request"));

        ResponseEntity<String> result = retryHandler.executeWithRetry(
                "openai", BASE_REQUEST
        );

        assertThat(result.getStatusCode().value()).isEqualTo(400);
        verify(providerGateway, times(1)).forward(any());
    }

    // ---------------------------------------------------------------
    // RETRY EXHAUSTION
    // ---------------------------------------------------------------

    @Test
    void shouldThrowRetryExhaustedWhenAllAttemptsReturn429() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "openai");
        ApiKey key3 = new ApiKey("sk-3", "openai");

        when(keyRotationService.getNextKey("openai"))
                .thenReturn(key1)
                .thenReturn(key2)
                .thenReturn(key3);

        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(429).body("limited"))
                .thenReturn(ResponseEntity.status(429).body("limited"))
                .thenReturn(ResponseEntity.status(429).body("limited"));

        assertThatThrownBy(() ->
                retryHandler.executeWithRetry("openai", BASE_REQUEST)
        )
                .isInstanceOf(RetryExhaustedException.class)
                .hasMessageContaining("openai")
                .hasMessageContaining("3");    // mentions attempt count

        // All 3 attempts were made
        verify(providerGateway, times(3)).forward(any());
        // All 3 keys marked as rate-limited
        verify(rateLimiterService).markAsRateLimited(key1.getId());
        verify(rateLimiterService).markAsRateLimited(key2.getId());
        verify(rateLimiterService).markAsRateLimited(key3.getId());
    }

    @Test
    void shouldThrowRetryExhaustedWhenNoKeysAvailableOnRetry() {
        ApiKey key = new ApiKey("sk-only-one", "openai");

        when(keyRotationService.getNextKey("openai"))
                .thenReturn(key)
                .thenThrow(new NoAvailableApiKeyException("No active API keys available"));

        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(429).body("limited"));

        assertThatThrownBy(() ->
                retryHandler.executeWithRetry("openai", BASE_REQUEST)
        )
                .isInstanceOf(RetryExhaustedException.class);
    }

    // ---------------------------------------------------------------
    // USAGE RECORDING
    // ---------------------------------------------------------------

    @Test
    void shouldRecordUsageOnlyForSuccessfulAttempts() {
        ApiKey limitedKey = new ApiKey("sk-limited", "openai");
        ApiKey goodKey    = new ApiKey("sk-good", "openai");

        when(keyRotationService.getNextKey("openai"))
                .thenReturn(limitedKey)
                .thenReturn(goodKey);

        when(providerGateway.forward(any()))
                .thenReturn(ResponseEntity.status(429).body("limited"))
                .thenReturn(ResponseEntity.ok("{}"));

        retryHandler.executeWithRetry("openai", BASE_REQUEST);

        // recordUsage only for the successful key
        verify(rateLimiterService, times(1)).recordUsage(goodKey.getId());
        verify(rateLimiterService, never()).recordUsage(limitedKey.getId());
    }

    @Test
    void shouldNotRecordUsageWhenProviderCommunicationFails() {
        ApiKey key = new ApiKey("sk-test", "openai");
        when(keyRotationService.getNextKey("openai")).thenReturn(key);
        when(providerGateway.forward(any()))
                .thenThrow(new ProviderCommunicationException("Network failure"));

        assertThatThrownBy(() ->
                retryHandler.executeWithRetry("openai", BASE_REQUEST)
        ).isInstanceOf(ProviderCommunicationException.class);

        verify(rateLimiterService, never()).recordUsage(any());
    }
}