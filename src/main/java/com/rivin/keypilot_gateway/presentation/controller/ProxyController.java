package com.rivin.keypilot_gateway.presentation.controller;


import com.rivin.keypilot_gateway.application.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProxyController {

    private final ProxyService proxyService;
    private final String defaultProvider;

    public ProxyController(
            ProxyService proxyService,
            @Value("${gateway.default-provider:openai}") String defaultProvider
    ) {
        this.proxyService = proxyService;
        this.defaultProvider = defaultProvider;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxy(
            HttpServletRequest request,
            @RequestHeader(value = "X-Gateway-Provider", required = false) String providerHeader,
            @RequestBody(required = false) String body) {

        String provider = resolveProvider(providerHeader);
        return proxyService.forward(provider, request, body);
    }

    private String resolveProvider(String providerHeader) {
        if (providerHeader != null && !providerHeader.isBlank()) {
            return providerHeader.toLowerCase();
        }
        return defaultProvider;
    }
}