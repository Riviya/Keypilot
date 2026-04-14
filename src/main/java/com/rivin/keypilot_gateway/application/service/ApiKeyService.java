package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.dto.AddApiKeyRequest;
import com.rivin.keypilot_gateway.application.dto.ApiKeyResponse;
import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.exception.InvalidIdException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public ApiKeyResponse addKey(AddApiKeyRequest request) {
        ApiKey apiKey = new ApiKey(request.keyValue(), request.provider());
        apiKeyRepository.save(apiKey);

        return toResponse(apiKey);
    }

    public List<ApiKeyResponse> listAllKeys() {
        List<ApiKey> apiKeys = apiKeyRepository.findAll();
        List<ApiKeyResponse> apiKeyResponses = new ArrayList<>();

        for (ApiKey apiKey : apiKeys) {
            apiKeyResponses.add(toResponse(apiKey));
        }
        return apiKeyResponses;
    }

    public void deleteKey(String id) {
        if (apiKeyRepository.findById(id).isEmpty()) {
            throw new InvalidIdException("ApiKey not found");
        }
        apiKeyRepository.delete(id);
    }


    private ApiKeyResponse toResponse(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getProvider(),
                apiKey.isActive()
        );
    }

}
