package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.dto.GatewayStatusResponse;
import com.rivin.keypilot_gateway.application.dto.ProviderStatusResponse;
import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.provider.ProviderRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GatewayStatusService {

    private final ProviderRegistry providerRegistry;
    private final ApiKeyRepository apiKeyRepository;

    public GatewayStatusService(ProviderRegistry providerRegistry,
                                ApiKeyRepository apiKeyRepository) {
        this.providerRegistry = providerRegistry;
        this.apiKeyRepository = apiKeyRepository;
    }

    public GatewayStatusResponse getStatus() {
        List<String> configuredProviders = providerRegistry.availableProviders();

        if (configuredProviders.isEmpty()) {
            return new GatewayStatusResponse(false, List.of());
        }

        List<ProviderStatusResponse> providerStatuses = configuredProviders.stream()
                .map(this::buildProviderStatus)
                .toList();

        boolean healthy = providerStatuses.stream()
                .anyMatch(ProviderStatusResponse::available);

        return new GatewayStatusResponse(healthy, providerStatuses);
    }

    private ProviderStatusResponse buildProviderStatus(String providerName) {
        List<ApiKey> allKeys = apiKeyRepository.findAllByProvider(providerName);

        int total       = allKeys.size();
        int inactive    = (int) allKeys.stream().filter(k -> !k.isActive()).count();
        int rateLimited = (int) allKeys.stream()
                .filter(ApiKey::isActive)
                .filter(ApiKey::isRateLimited)
                .count();
        int available   = total - inactive - rateLimited;

        boolean providerAvailable = available > 0;

        return new ProviderStatusResponse(
                providerName,
                providerAvailable,
                total,
                available,
                rateLimited,
                inactive
        );
    }
}