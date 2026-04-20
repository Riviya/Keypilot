package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.Exception.RetryExhaustedException;
import com.rivin.keypilot_gateway.application.retry.RetryPolicy;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.proxy.ProviderGateway;
import com.rivin.keypilot_gateway.domain.proxy.ProxyRequest;
import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RetryHandler {

    private static final Logger log = LoggerFactory.getLogger(RetryHandler.class);

    private final KeyRotationService keyRotationService;
    private final RateLimiterService rateLimiterService;
    private final ProviderGateway providerGateway;
    private final RetryPolicy retryPolicy;

    public RetryHandler(KeyRotationService keyRotationService,
                        RateLimiterService rateLimiterService,
                        ProviderGateway providerGateway,
                        RetryPolicy retryPolicy) {
        this.keyRotationService = keyRotationService;
        this.rateLimiterService = rateLimiterService;
        this.providerGateway    = providerGateway;
        this.retryPolicy        = retryPolicy;
    }

    public ResponseEntity<String> executeWithRetry(String provider,
                                                   ProxyRequest baseRequest) {
        int attempt = 1;

        while (retryPolicy.hasAttemptsRemaining(attempt)) {

            // Pick the next available key for this attempt
            ApiKey key;
            try {
                key = keyRotationService.getNextKey(provider);
            } catch (NoAvailableApiKeyException ex) {
                // No keys left to try — give up
                throw new RetryExhaustedException(provider, attempt - 1);
            }

            // Build a new request with this attempt's key injected
            ProxyRequest attemptRequest = injectKey(baseRequest, key.getKeyValue());

            log.info("Proxy attempt={} provider={} keyId={}",
                    attempt, provider, key.getId()
            );

            ResponseEntity<String> response = providerGateway.forward(attemptRequest);
            int statusCode = response.getStatusCode().value();

            if (!retryPolicy.isRetryable(statusCode)) {
                // Success or non-retryable error — record usage and return
                if (statusCode >= 200 && statusCode < 300) {
                    rateLimiterService.recordUsage(key.getId());
                }
                return response;
            }

            // 429 — mark this key as exhausted and try again
            log.warn("Provider returned 429 attempt={} provider={} keyId={}",
                    attempt, provider, key.getId()
            );
            rateLimiterService.markAsRateLimited(key.getId());
            attempt++;
        }

        throw new RetryExhaustedException(provider, retryPolicy.maxAttempts());
    }

    private ProxyRequest injectKey(ProxyRequest base, String apiKey) {
        return new ProxyRequest(
                base.provider(),
                base.baseUrl(),
                base.path(),
                base.method(),
                apiKey,             // ← replaced with this attempt's key
                base.headers(),
                base.body()
        );
    }
}