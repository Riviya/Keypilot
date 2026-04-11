package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.rotation.KeyRotationStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeyRotationService {

    private final ApiKeyRepository apiKeyRepository;
    private final KeyRotationStrategy keyRotationStrategy;
    private final RateLimiterService rateLimiterService;

    public KeyRotationService(
            ApiKeyRepository apiKeyRepository,
            @Qualifier("roundRobin") KeyRotationStrategy keyRotationStrategy,
            RateLimiterService rateLimiterService
            ) {
        this.apiKeyRepository = apiKeyRepository;
        this.keyRotationStrategy = keyRotationStrategy;
        this.rateLimiterService = rateLimiterService;
    }

    public ApiKey getNextKey(String provider) {
        List<ApiKey> keys = rateLimiterService.getAvailableKeys(provider);
        return keyRotationStrategy.selectKey(keys)
                ;
    }
}