package com.rivin.keypilot_gateway.infrastructure.proxy.auth;


import org.springframework.web.util.UriComponentsBuilder;

public class QueryParamAuthStrategy implements ProviderAuthStrategy {

    private final String paramName;

    public QueryParamAuthStrategy(String paramName) {
        this.paramName = paramName;
    }

    @Override
    public String buildAuthenticatedUrl(String url, String apiKey) {
        return UriComponentsBuilder.fromUriString(url)
                .queryParam(paramName, apiKey)
                .toUriString();
    }
    // apply() not overridden — no Authorization header added
}