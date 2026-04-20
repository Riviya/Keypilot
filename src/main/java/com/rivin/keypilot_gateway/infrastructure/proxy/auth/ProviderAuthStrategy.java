package com.rivin.keypilot_gateway.infrastructure.proxy.auth;


import org.springframework.http.HttpHeaders;

public interface ProviderAuthStrategy {

    /**
     * Apply authentication to outbound HTTP headers.
     * Used by BEARER and API_KEY_HEADER strategies.
     */
    default void apply(HttpHeaders headers, String apiKey) {
        // no-op by default — QueryParamStrategy overrides buildAuthenticatedUrl instead
    }

    /**
     * Build an authenticated URL by appending the key as a query parameter.
     * Used by QUERY_PARAM strategy.
     * Default implementation returns the URL unchanged.
     */
    default String buildAuthenticatedUrl(String url, String apiKey) {
        return url;
    }
}
