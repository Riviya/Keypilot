package com.rivin.keypilot_gateway.infrastructure.Exception;


public class UnknownAuthStrategyException extends RuntimeException {
    public UnknownAuthStrategyException(String provider) {
        super("No auth strategy configured for provider: " + provider);
    }
}