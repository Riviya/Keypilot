package com.rivin.keypilot_gateway.infrastructure.proxy.auth;

import org.springframework.http.HttpHeaders;

public class BearerAuthStrategy implements ProviderAuthStrategy {

    @Override
    public void apply(HttpHeaders headers, String apiKey) {
        headers.set("Authorization", "Bearer " + apiKey);
    }
    // buildAuthenticatedUrl not overridden — URL stays clean
}