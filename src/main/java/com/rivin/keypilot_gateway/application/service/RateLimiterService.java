package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.dto.ApiKeyResponse;
import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.exception.InvalidApiException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RateLimiterService {


    private final ApiKeyRepository apiKeyRepository;

    public RateLimiterService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public void recordUsage(String id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new InvalidApiException(
                        "No api key found with id: " + id
                ));

        apiKey.recordRequest();
    }

    public List<ApiKey> getAvailableKeys(String provider) {
        List<ApiKey> apiKeys = apiKeyRepository.findAllByProvider(provider);
        List<ApiKey> filteredKeys = new ArrayList<>();

        for (ApiKey apiKey : apiKeys) {
            if (apiKey.isActive() && !apiKey.isRateLimited()){
                filteredKeys.add(apiKey);
            }
        }
        return filteredKeys;
    }


}
