package com.rivin.keypilot_gateway.infrastructure.persistence;


import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryApiKeyRepository implements ApiKeyRepository {

    private final Map<String, ApiKey> store = new ConcurrentHashMap<>();

    @Override
    public void save(ApiKey apiKey) {
        store.put(apiKey.getId(), apiKey);
    }

    @Override
    public Optional<ApiKey> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ApiKey> findAllByProvider(String provider) {
        return store.values().stream()
                .filter(k -> k.getProvider().equalsIgnoreCase(provider))
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiKey> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }
}