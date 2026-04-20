package com.rivin.keypilot_gateway.domain.provider;

import com.rivin.keypilot_gateway.infrastructure.proxy.InMemoryProviderRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ProviderRegistryTest {

    private InMemoryProviderRegistry buildRegistry(Map<String, String> providers) {
        return new InMemoryProviderRegistry(providers);
    }

    // ---------------------------------------------------------------
    // RESOLVE PROVIDER
    // ---------------------------------------------------------------

    @Test
    void shouldResolveConfiguredProvider() {
        InMemoryProviderRegistry registry = buildRegistry(Map.of(
                "openai", "https://api.openai.com"
        ));

        Optional<Provider> result = registry.resolve("openai");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("openai");
        assertThat(result.get().baseUrl()).isEqualTo("https://api.openai.com");
    }

    @Test
    void shouldReturnEmptyForUnknownProvider() {
        InMemoryProviderRegistry registry = buildRegistry(Map.of(
                "openai", "https://api.openai.com"
        ));

        Optional<Provider> result = registry.resolve("unknown-provider");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldResolveProviderCaseInsensitively() {
        InMemoryProviderRegistry registry = buildRegistry(Map.of(
                "openai", "https://api.openai.com"
        ));

        assertThat(registry.resolve("OpenAI")).isPresent();
        assertThat(registry.resolve("OPENAI")).isPresent();
        assertThat(registry.resolve("openai")).isPresent();
    }

    @Test
    void shouldSupportMultipleProviders() {
        InMemoryProviderRegistry registry = buildRegistry(Map.of(
                "openai",    "https://api.openai.com",
                "anthropic", "https://api.anthropic.com"
        ));

        assertThat(registry.resolve("openai")).isPresent();
        assertThat(registry.resolve("anthropic")).isPresent();
    }

    @Test
    void shouldReturnAllRegisteredProviderNames() {
        InMemoryProviderRegistry registry = buildRegistry(Map.of(
                "openai",    "https://api.openai.com",
                "anthropic", "https://api.anthropic.com"
        ));

        assertThat(registry.availableProviders())
                .containsExactlyInAnyOrder("openai", "anthropic");
    }

    @Test
    void shouldReturnEmptyListWhenNoProvidersConfigured() {
        InMemoryProviderRegistry registry = buildRegistry(Map.of());

        assertThat(registry.availableProviders()).isEmpty();
    }
}