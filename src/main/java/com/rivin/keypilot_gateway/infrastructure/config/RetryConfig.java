package com.rivin.keypilot_gateway.infrastructure.config;

import com.rivin.keypilot_gateway.application.retry.RetryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {

    @Value("${gateway.retry.max-attempts:3}")
    private int maxAttempts;

    @Bean
    public RetryPolicy retryPolicy() {
        return new RetryPolicy(maxAttempts);
    }
}