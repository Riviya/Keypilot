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

    private final ProviderRegistry providerRegistry;
    private final RetryHandler retryHandler;

    public ProxyService(ProviderRegistry providerRegistry, RetryHandler retryHandler) {
        this.providerRegistry = providerRegistry;
        this.retryHandler     = retryHandler;
    }

    public ResponseEntity<String> forward(String providerName, HttpServletRequest request, String body) {
        Provider provider = providerRegistry.resolve(providerName)
                .orElseThrow(() -> new ProviderNotFoundException(providerName));

        // Build the base request — apiKey is a placeholder;
        // RetryHandler will inject the real key per attempt
        ProxyRequest baseRequest = new ProxyRequest(
                providerName,
                provider.baseUrl(),
                request.getRequestURI(),
                HttpMethod.valueOf(request.getMethod()),
                "",                         // placeholder — RetryHandler replaces this
                extractHeaders(request),
                body
        );

        return retryHandler.executeWithRetry(providerName, baseRequest);
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> {
            if (!name.equalsIgnoreCase("Authorization") &&
                    !name.equalsIgnoreCase("X-Gateway-Provider")) {
                headers.put(name, request.getHeader(name));
            }
        });
        return headers;
    }
}