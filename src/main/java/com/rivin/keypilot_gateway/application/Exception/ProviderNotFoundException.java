package com.rivin.keypilot_gateway.application.Exception;

public class ProviderNotFoundException extends RuntimeException {
    public ProviderNotFoundException(String providerName) {
        super("Provider not configured: " + providerName);
    }
}