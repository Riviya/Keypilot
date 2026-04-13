package com.rivin.keypilot_gateway.infrastructure.Exception;


public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}