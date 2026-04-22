package com.rivin.keypilot_gateway.infrastructure.config;



import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.infrastructure.persistence.FileBackedApiKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PersistenceConfig {

    @Value("${keypilot.storage.path=${user.home}/.cli/keys.json}")
    private String storagePath;

    @Bean
    @Primary  // ← tells Spring: use this over InMemoryApiKeyRepository
    public ApiKeyRepository fileBackedApiKeyRepository() {
        return new FileBackedApiKeyRepository(storagePath);
    }
}