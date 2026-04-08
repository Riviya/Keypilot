package com.rivin.keypilot_gateway.domain.rotation;


import com.rivin.keypilot_gateway.domain.model.ApiKey;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinRotationStrategy implements RotationStrategy {

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @Override
    public ApiKey nextKey(List<ApiKey> keys) {

        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("No API keys available for rotation");
        }

        int index = currentIndex.getAndUpdate(i -> (i + 1) % keys.size());
        return keys.get(index);
    }
}