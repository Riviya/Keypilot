package com.rivin.keypilot_gateway.infrastructure.proxy;

import com.rivin.keypilot_gateway.domain.provider.Provider;
import com.rivin.keypilot_gateway.domain.provider.ProviderRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryProviderRegistry implements ProviderRegistry {

    private final Map<String, Provider> providers;

    public InMemoryProviderRegistry(Map<String, String> providerBaseUrls) {
        this.providers = providerBaseUrls.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toLowerCase(),
                        e -> new Provider(e.getKey().toLowerCase(), e.getValue())
                ));
    }

    @Override
    public Optional<Provider> resolve(String providerName) {
        if (providerName == null) return Optional.empty();
        return Optional.ofNullable(providers.get(providerName.toLowerCase()));
    }

    @Override
    public List<String> availableProviders() {
        return List.copyOf(providers.keySet());
    }
}
