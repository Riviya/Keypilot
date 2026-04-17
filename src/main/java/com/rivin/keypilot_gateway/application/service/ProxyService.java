package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.Exception.ProviderNotFoundException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.provider.Provider;
import com.rivin.keypilot_gateway.domain.provider.ProviderRegistry;
import com.rivin.keypilot_gateway.domain.proxy.ProviderGateway;
import com.rivin.keypilot_gateway.domain.proxy.ProxyRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProxyService {

    private final KeyRotationService keyRotationService;
    private final ProviderGateway providerGateway;
    private final RateLimiterService rateLimiterService;
    private final ProviderRegistry providerRegistry;

    public ProxyService(KeyRotationService keyRotationService, ProviderGateway providerGateway, RateLimiterService rateLimiterService, ProviderRegistry providerRegistry) {
        this.keyRotationService = keyRotationService;
        this.providerGateway = providerGateway;
        this.rateLimiterService = rateLimiterService;
        this.providerRegistry = providerRegistry;
    }

    public ResponseEntity<String> forward(String providerName,
                                          HttpServletRequest request,
                                          String body) {

        // Resolve provider — throws if not configured
        Provider provider = providerRegistry.resolve(providerName)
                .orElseThrow(() -> new ProviderNotFoundException(providerName));

        ApiKey selectedKey = keyRotationService.getNextKey(providerName);
        Map<String, String> headers = extractHeaders(request);

        ProxyRequest proxyRequest = new ProxyRequest(
                providerName,
                provider.baseUrl(),
                request.getRequestURI(),
                HttpMethod.valueOf(request.getMethod()),
                selectedKey.getKeyValue(),
                headers,
                body
        );

        try {
            // Forward request
            ResponseEntity<String> response = providerGateway.forward(proxyRequest);

            // ✅ Record usage ONLY if request reached provider (no exception)
            rateLimiterService.recordUsage(selectedKey.getId());

            return response;

        } catch (Exception ex) {
            // ❌ Do NOT record usage if request failed to reach provider
            throw ex;
        }
    }


    // Extract and remove Authorization header
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();


        Collections.list(request.getHeaderNames()).forEach(name -> {
            // Never forward the caller's Authorization header
            // The gateway always injects its own managed key
            if (!name.equalsIgnoreCase("Authorization") &&
                    !name.equalsIgnoreCase("X-Gateway-Provider")
            ) {
                headers.put(name, request.getHeader(name));
            }
        });

        return headers;
    }
}