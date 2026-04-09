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

    public KeyRotationService(
            ApiKeyRepository apiKeyRepository,
            @Qualifier("roundRobin") KeyRotationStrategy keyRotationStrategy
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.keyRotationStrategy = keyRotationStrategy;
    }

    public ApiKey getNextKey(String provider) {
        List<ApiKey> keys = apiKeyRepository.findAllByProvider(provider);
        return keyRotationStrategy.selectKey(keys)
                ;
    }
}