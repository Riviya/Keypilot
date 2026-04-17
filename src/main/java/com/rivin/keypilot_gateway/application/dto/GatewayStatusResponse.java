package com.rivin.keypilot_gateway.application.dto;

import java.util.List;

public record GatewayStatusResponse(
        boolean healthy,
        List<ProviderStatusResponse> providers
) {}