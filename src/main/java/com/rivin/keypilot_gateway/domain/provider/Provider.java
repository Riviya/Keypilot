package com.rivin.keypilot_gateway.domain.provider;

public record Provider(String name, String baseUrl) {

    public Provider {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Provider name must not be blank");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Provider baseUrl must not be blank");
        }
    }
}