package com.rivin.keypilot_gateway.domain.provider;

import java.util.List;
import java.util.Optional;

public interface ProviderRegistry {
    Optional<Provider> resolve(String providerName);
    List<String> availableProviders();
}