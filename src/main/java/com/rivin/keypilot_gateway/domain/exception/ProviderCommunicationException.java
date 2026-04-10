package com.rivin.keypilot_gateway.domain.exception;


public class ProviderCommunicationException extends RuntimeException {
    public ProviderCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderCommunicationException(String message) {
        super(message);
    }
}