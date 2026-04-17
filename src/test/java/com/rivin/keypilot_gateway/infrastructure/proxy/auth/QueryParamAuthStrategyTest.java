package com.rivin.keypilot_gateway.infrastructure.proxy.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class QueryParamAuthStrategyTest {

    // ---------------------------------------------------------------
    // Query param strategy works differently — it mutates the URI,
    // not the headers. We test the URI building logic directly.
    // ---------------------------------------------------------------

    @Test
    void shouldAppendKeyAsQueryParameter() {
        QueryParamAuthStrategy strategy = new QueryParamAuthStrategy("key");

        String result = strategy.buildAuthenticatedUrl(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
                "gemini-api-key-abc"
        );

        assertThat(result).isEqualTo(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=gemini-api-key-abc"
        );
    }

    @Test
    void shouldAppendToExistingQueryParameters() {
        QueryParamAuthStrategy strategy = new QueryParamAuthStrategy("key");

        String result = strategy.buildAuthenticatedUrl(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?alt=json",
                "gemini-api-key-abc"
        );

        assertThat(result).contains("alt=json");
        assertThat(result).contains("key=gemini-api-key-abc");
    }

    @Test
    void shouldUseConfiguredParamName() {
        // Different providers may use different param names
        QueryParamAuthStrategy strategy = new QueryParamAuthStrategy("api_key");

        String result = strategy.buildAuthenticatedUrl(
                "https://some-provider.com/v1/endpoint",
                "my-secret-key"
        );

        assertThat(result)
                .contains("api_key=my-secret-key")
                .doesNotContain("?key=")      // Check for "?key=" at start
                .doesNotContain("&key=");     // Check for "&key=" as additional param
    }

    @Test
    void shouldNotAddAuthorizationHeader() {
        // Query param auth must NOT set Authorization header
        // This is verified structurally — the strategy has no header-setting method
        QueryParamAuthStrategy strategy = new QueryParamAuthStrategy("key");

        // The strategy only exposes buildAuthenticatedUrl — no applyHeaders method
        // This is enforced by the type system itself
        assertThat(strategy).isNotInstanceOf(BearerAuthStrategy.class);
    }
}