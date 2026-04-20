package com.rivin.keypilot_gateway.infrastructure.proxy.auth;

import org.springframework.http.HttpHeaders;

public class ApiKeyHeaderAuthStrategy implements ProviderAuthStrategy {

    private final String headerName;

    public ApiKeyHeaderAuthStrategy(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public void apply(HttpHeaders headers, String apiKey) {
        headers.set(headerName, apiKey); // raw value, no "Bearer " prefix
    }
}