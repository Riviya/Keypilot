package com.rivin.keypilot_gateway.infrastructure.config;


import com.rivin.keypilot_gateway.domain.provider.ProviderRegistry;
import com.rivin.keypilot_gateway.infrastructure.proxy.InMemoryProviderRegistry;
import com.rivin.keypilot_gateway.infrastructure.proxy.auth.ProviderAuthConfig;
import com.rivin.keypilot_gateway.infrastructure.proxy.auth.ProviderAuthStrategyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderConfig {

    @Bean
    public ProviderRegistry providerRegistry(ProviderProperties properties) {

        Map<String, String> urlMap = properties.getProviders().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getBaseUrl()
                ));

        return new InMemoryProviderRegistry(urlMap);
    }

    @Bean
    public ProviderAuthStrategyFactory providerAuthStrategyFactory(
            ProviderProperties properties) {

        Map<String, ProviderAuthConfig> configs =
                properties.getProviders().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> new ProviderAuthConfig(
                                        e.getValue().getAuthType(),
                                        e.getValue().getAuthParamName()
                                )
                        ));

        return new ProviderAuthStrategyFactory(configs);
    }
}