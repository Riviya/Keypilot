package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.domain.model.ApiKey;
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

    public ProxyService(KeyRotationService keyRotationService, ProviderGateway providerGateway) {
        this.keyRotationService = keyRotationService;
        this.providerGateway = providerGateway;
    }

    public ResponseEntity<String> forward(String provider, HttpServletRequest request, String body) {

        // 1. Get the correct API key
        ApiKey selectedKey = keyRotationService.getNextKey(provider);

        // 2. Extract the header details in the request (Without Authorization)
        Map<String, String> headers = extractHeaders(request);

        // Create a new proxy request so that we can transfer extracted data
        ProxyRequest proxyRequest = new ProxyRequest(
                provider,
                request.getRequestURI(),
                HttpMethod.valueOf(request.getMethod()),
                selectedKey.getKeyValue(),
                headers,
                body
        );

        return providerGateway.forward(proxyRequest);
    }


    // Extract and remove Authorization header
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();


        Collections.list(request.getHeaderNames()).forEach(name -> {
            // Never forward the caller's Authorization header
            // The gateway always injects its own managed key
            if (!name.equalsIgnoreCase("Authorization")) {
                headers.put(name, request.getHeader(name));
            }
        });

        return headers;
    }
}