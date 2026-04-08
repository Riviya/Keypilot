package com.rivin.keypilot_gateway.domain.exception;


public class InvalidApiProviderException extends RuntimeException {
    public InvalidApiProviderException(String message) {
        super(message);
    }
}