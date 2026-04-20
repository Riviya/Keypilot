package com.rivin.keypilot_gateway.presentation.exception;

import com.rivin.keypilot_gateway.application.Exception.ProviderNotFoundException;
import com.rivin.keypilot_gateway.application.Exception.RetryExhaustedException;
import com.rivin.keypilot_gateway.domain.exception.InvalidIdException;
import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import com.rivin.keypilot_gateway.domain.exception.ProviderCommunicationException;
import com.rivin.keypilot_gateway.infrastructure.Exception.StorageException;
import com.rivin.keypilot_gateway.infrastructure.Exception.UnknownAuthStrategyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ProviderCommunicationException.class)
    public ResponseEntity<?> handle(ProviderCommunicationException ex) {

        if (ex.getMessage().contains("all limited")) {
            return ResponseEntity.status(503).body(
                    Map.of("error", "Service Unavailable", "provider", "openai")
            );
        }

        if (ex.getMessage().contains("rate limited")) {
            return ResponseEntity.status(503).body(
                    Map.of("error", "Service Unavailable", "provider", "openai")
            );
        }

        return ResponseEntity.status(502).body(
                Map.of("error", ex.getMessage())
        );
    }

    @ExceptionHandler(NoAvailableApiKeyException.class)
    public ResponseEntity<Map<String, String>> handleNoAvailableApiKeyException(
            NoAvailableApiKeyException ex){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, String>> handleStorageFailure(StorageException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Storage failure: " + ex.getMessage()));
    }

    @ExceptionHandler(ProviderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProviderNotFound(
            ProviderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UnknownAuthStrategyException.class)
    public ResponseEntity<Map<String, String>> handleUnknownAuth(
            UnknownAuthStrategyException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RetryExhaustedException.class)
    public ResponseEntity<Map<String, String>> handleRetryExhausted(
            RetryExhaustedException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "provider", ex.getProvider(),
                        "attemptsExhausted", String.valueOf(ex.getAttempts())
                ));
    }

    @ExceptionHandler(InvalidIdException.class)
    public ResponseEntity<Map<String, String>> handleInvalidId(InvalidIdException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}