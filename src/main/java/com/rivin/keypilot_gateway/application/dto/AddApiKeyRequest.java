package com.rivin.keypilot_gateway.application.dto;


import jakarta.validation.constraints.NotBlank;

public record AddApiKeyRequest(
        @NotBlank(message = "provider must not be blank") String provider,
        @NotBlank(message = "keyValue must not be blank") String keyValue
) {}