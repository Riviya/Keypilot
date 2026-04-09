package com.rivin.keypilot_gateway.domain.exception;

public class NoAvailableApiKeyException extends RuntimeException {
    public NoAvailableApiKeyException(String message) {
        super(message);
    }
}