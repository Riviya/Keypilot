package com.rivin.keypilot_gateway.infrastructure.proxy.auth;

import com.rivin.keypilot_gateway.infrastructure.Exception.UnknownAuthStrategyException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProviderAuthStrategyFactory {

    private final Map<String, ProviderAuthStrategy> strategies;

    public ProviderAuthStrategyFactory(Map<String, ProviderAuthConfig> configs) {
        this.strategies = configs.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toLowerCase(),
                        e -> buildStrategy(e.getValue())
                ));
    }

    public ProviderAuthStrategy getStrategy(String providerName) {
        if (providerName == null) {
            throw new UnknownAuthStrategyException("null");
        }

        ProviderAuthStrategy strategy = strategies.get(providerName.toLowerCase());

        if (strategy == null) {
            throw new UnknownAuthStrategyException(providerName);
        }

        return strategy;
    }

    private ProviderAuthStrategy buildStrategy(ProviderAuthConfig config) {
        return switch (config.authType()) {
            case BEARER        -> new BearerAuthStrategy();
            case QUERY_PARAM   -> new QueryParamAuthStrategy(config.paramName());
            case API_KEY_HEADER -> new ApiKeyHeaderAuthStrategy(config.paramName());
        };
    }
}
