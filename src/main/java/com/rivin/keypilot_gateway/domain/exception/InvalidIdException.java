package com.rivin.keypilot_gateway.domain.exception;

public class InvalidIdException extends RuntimeException {
    public InvalidIdException(String message) {
        super(message);
    }
}