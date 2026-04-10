package com.rivin.keypilot_gateway.presentation.exception;

import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import com.rivin.keypilot_gateway.domain.exception.ProviderCommunicationException;
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
    public ResponseEntity<Map<String, String>> handleProviderFailure(
            ProviderCommunicationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoAvailableApiKeyException.class)
    public ResponseEntity<Map<String, String>> handleNoAvailableApiKeyException(
            NoAvailableApiKeyException ex){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }
}