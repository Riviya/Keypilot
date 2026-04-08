package com.rivin.keypilot_gateway.domain.rotation;

import com.rivin.keypilot_gateway.domain.model.ApiKey;

import java.util.List;

public interface RotationStrategy {
    ApiKey nextKey(List<ApiKey> keys);
}