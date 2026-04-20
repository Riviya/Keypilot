package com.rivin.keypilot_gateway.domain.proxy;

import org.springframework.http.HttpMethod;

import java.util.Map;

public record ProxyRequest(
        String provider,
        String baseUrl,
        String path,
        HttpMethod method,
        String apiKey,
        Map<String, String> headers,
        String body
) {}