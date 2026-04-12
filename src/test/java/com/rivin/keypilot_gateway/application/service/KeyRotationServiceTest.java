package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.rotation.KeyRotationStrategy;
import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyRotationServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private KeyRotationStrategy keyRotationStrategy;

    @Mock
    private RateLimiterService rateLimiterService;

    private KeyRotationService keyRotationService;

    @BeforeEach
    void setUp() {
        keyRotationService = new KeyRotationService(apiKeyRepository, keyRotationStrategy,  rateLimiterService);
    }

    @Test
    void shouldFetchKeysByProviderAndDelegateToStrategy() {
        String provider = "openai";
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "openai");
        List<ApiKey> keys = List.of(key1, key2);


        when(rateLimiterService.getAvailableKeys(provider)).thenReturn(keys);
        when(keyRotationStrategy.selectKey(keys)).thenReturn(key1);

        ApiKey selected = keyRotationService.getNextKey(provider);

        assertThat(selected).isEqualTo(key1);
        verify(rateLimiterService).getAvailableKeys(provider);
        verify(keyRotationStrategy).selectKey(keys);
    }

    @Test
    void shouldPropagateExceptionWhenNoKeysAvailable() {
        String provider = "openai";
        when(rateLimiterService.getAvailableKeys(provider)).thenReturn(List.of());

        when(keyRotationStrategy.selectKey(List.of()))
                .thenThrow(new NoAvailableApiKeyException("No active API keys available"));

        assertThatThrownBy(() -> keyRotationService.getNextKey(provider))
                .isInstanceOf(NoAvailableApiKeyException.class);
    }

    @Test
    void shouldNotHardcodeStrategyLogicInService() {
        // The service must fully delegate — never reimplement selection logic.
        // This test verifies strategy.selectKey() is always called.
        String provider = "anthropic";
        ApiKey key = new ApiKey(provider, "sk-anth-1");

        when(rateLimiterService.getAvailableKeys(provider)).thenReturn(List.of(key));

        when(keyRotationStrategy.selectKey(any())).thenReturn(key);

        keyRotationService.getNextKey(provider);

        verify(keyRotationStrategy, times(1)).selectKey(any());
    }

    @Test
    void shouldOnlyPassAvailableKeysToRotationStrategy() {
        String provider = "openai";
        ApiKey availableKey = new ApiKey("sk-available", provider, 10, 60);
        List<ApiKey> availableKeys = List.of(availableKey);

        when(rateLimiterService.getAvailableKeys(provider)).thenReturn(availableKeys);
        when(keyRotationStrategy.selectKey(availableKeys)).thenReturn(availableKey);

        ApiKey result = keyRotationService.getNextKey(provider);

        assertThat(result).isEqualTo(availableKey);
        verify(rateLimiterService).getAvailableKeys(provider);
        // Repository.findAllByProvider should no longer be called directly
        verify(apiKeyRepository, never()).findAllByProvider(any());
    }
}