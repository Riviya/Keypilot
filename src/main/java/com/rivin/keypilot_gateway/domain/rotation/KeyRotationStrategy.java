package com.rivin.keypilot_gateway.domain.rotation;

import com.rivin.keypilot_gateway.domain.model.ApiKey;

import java.util.List;

public interface KeyRotationStrategy {
    ApiKey selectKey(List<ApiKey> availableKeys);
}