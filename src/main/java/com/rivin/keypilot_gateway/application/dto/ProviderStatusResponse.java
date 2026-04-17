package com.rivin.keypilot_gateway.application.dto;

public record ProviderStatusResponse(
        String providerName,
        boolean available,
        int totalKeys,
        int availableKeys,
        int rateLimitedKeys,
        int inactiveKeys
) {}
