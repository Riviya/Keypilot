package com.rivin.keypilot_gateway.domain.rotation;

import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component("random")
public class RandomRotationStrategy extends BaseKeyRotationStrategy {

    private final Random random = new Random();

    @Override
    public ApiKey selectKey(List<ApiKey> availableKeys) {
        List<ApiKey> activeKeys = getActiveKeys(availableKeys);
        return activeKeys.get(random.nextInt(activeKeys.size()));
    }
}