package com.rivin.keypilot_gateway.domain.rotation;


import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component("roundRobin")
public class RoundRobinRotationStrategy extends BaseKeyRotationStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public ApiKey selectKey(List<ApiKey> availableKeys) {
        List<ApiKey> activeKeys = getActiveKeys(availableKeys);
        int index = counter.getAndIncrement() % activeKeys.size();
        return activeKeys.get(index);
    }
}