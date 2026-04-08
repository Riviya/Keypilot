package com.rivin.keypilot_gateway.domain.exception;

public class InvalidApiException extends RuntimeException {
    public InvalidApiException(String message) {
        super(message);
    }
}