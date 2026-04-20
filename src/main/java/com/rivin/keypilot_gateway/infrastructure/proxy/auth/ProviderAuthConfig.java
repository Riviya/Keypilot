package com.rivin.keypilot_gateway.infrastructure.proxy.auth;


// paramName is only used for QUERY_PARAM and API_KEY_HEADER strategies
// For BEARER it is null
public record ProviderAuthConfig(AuthType authType, String paramName) {

    public ProviderAuthConfig {
        if (authType == null) {
            authType = AuthType.BEARER; // safe default
        }
    }
}