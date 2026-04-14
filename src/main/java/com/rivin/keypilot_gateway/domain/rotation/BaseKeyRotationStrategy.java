package com.rivin.keypilot_gateway.domain.rotation;


import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;

import java.util.List;

abstract class BaseKeyRotationStrategy implements KeyRotationStrategy {

    protected List<ApiKey> getActiveKeys(List<ApiKey> keys) {
        List<ApiKey> active = keys.stream()
                .filter(ApiKey::isActive)
                .toList();
        if (active.isEmpty()) {
            throw new NoAvailableApiKeyException("No active API keys available");
        }
        return active;
    }
}