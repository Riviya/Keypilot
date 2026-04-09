package com.rivin.keypilot_gateway.presentation.controller;

import com.rivin.keypilot_gateway.application.dto.AddApiKeyRequest;
import com.rivin.keypilot_gateway.application.dto.ApiKeyResponse;
import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.application.service.ApiKeyService;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/keys")
public class ApiKeyController {


    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey (@Valid @RequestBody AddApiKeyRequest request) {
        ApiKeyResponse response = apiKeyService.addKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getAllKeys() {
        List<ApiKeyResponse> response = apiKeyService.listAllKeys();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable String id) {
        apiKeyService.deleteKey(id);
        return ResponseEntity.noContent().build();
    }

}
