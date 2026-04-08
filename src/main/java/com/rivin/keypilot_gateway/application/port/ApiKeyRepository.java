package com.rivin.keypilot_gateway.application.port;


import com.rivin.keypilot_gateway.domain.model.ApiKey;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository {
    void save(ApiKey apiKey);
    Optional<ApiKey> findById(String id);
    List<ApiKey> findAllByProvider(String provider);
    List<ApiKey> findAll();
    void delete(String id);
}