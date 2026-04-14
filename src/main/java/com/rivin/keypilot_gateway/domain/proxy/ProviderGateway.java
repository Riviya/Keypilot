package com.rivin.keypilot_gateway.domain.proxy;

import org.springframework.http.ResponseEntity;

public interface ProviderGateway {
    ResponseEntity<String> forward(ProxyRequest request);
}