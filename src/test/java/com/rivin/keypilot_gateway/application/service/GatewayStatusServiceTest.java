package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.dto.GatewayStatusResponse;
import com.rivin.keypilot_gateway.application.dto.ProviderStatusResponse;
import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.provider.ProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayStatusServiceTest {

    @Mock private ProviderRegistry providerRegistry;
    @Mock private ApiKeyRepository apiKeyRepository;

    private GatewayStatusService statusService;

    @BeforeEach
    void setUp() {
        statusService = new GatewayStatusService(providerRegistry, apiKeyRepository);
    }

    // ---------------------------------------------------------------
    // OVERALL STATUS
    // ---------------------------------------------------------------

    @Test
    void shouldReportHealthyWhenProvidersConfiguredAndKeysAvailable() {
        when(providerRegistry.availableProviders()).thenReturn(List.of("openai"));
        when(apiKeyRepository.findAllByProvider("openai")).thenReturn(
                List.of(new ApiKey("openai", "sk-test", 10, 60))
        );

        GatewayStatusResponse status = statusService.getStatus();

        assertThat(status.healthy()).isTrue();
    }

    @Test
    void shouldReportUnhealthyWhenNoProvidersConfigured() {
        when(providerRegistry.availableProviders()).thenReturn(List.of());

        GatewayStatusResponse status = statusService.getStatus();

        assertThat(status.healthy()).isFalse();
    }

    @Test
    void shouldReportUnhealthyWhenAllKeysRateLimited() {
        when(providerRegistry.availableProviders()).thenReturn(List.of("openai"));

        ApiKey limited = new ApiKey("openai", "sk-limited", 1, 60);
        limited.recordRequest(); // hits limit

        when(apiKeyRepository.findAllByProvider("openai")).thenReturn(List.of(limited));

        GatewayStatusResponse status = statusService.getStatus();

        assertThat(status.healthy()).isFalse();
    }

    // ---------------------------------------------------------------
    // PROVIDER STATUS
    // ---------------------------------------------------------------

    @Test
    void shouldIncludeStatusForEachConfiguredProvider() {
        when(providerRegistry.availableProviders())
                .thenReturn(List.of("openai", "anthropic"));
        when(apiKeyRepository.findAllByProvider("openai"))
                .thenReturn(List.of(new ApiKey("openai", "sk-1", 10, 60)));
        when(apiKeyRepository.findAllByProvider("anthropic"))
                .thenReturn(List.of(new ApiKey("anthropic", "sk-2", 10, 60)));

        GatewayStatusResponse status = statusService.getStatus();

        assertThat(status.providers()).hasSize(2);
        assertThat(status.providers())
                .extracting(ProviderStatusResponse::providerName)
                .containsExactlyInAnyOrder("openai", "anthropic");
    }

    @Test
    void shouldCountTotalAndAvailableKeysPerProvider() {
        when(providerRegistry.availableProviders()).thenReturn(List.of("openai"));

        ApiKey available = new ApiKey("openai", "sk-ok",      10, 60);
        ApiKey limited   = new ApiKey("openai", "sk-limited",  1, 60);
        ApiKey inactive  = new ApiKey("openai", "sk-inactive", 10, 60);
        limited.recordRequest(); // rate-limited
        inactive.deactivate();   // inactive

        when(apiKeyRepository.findAllByProvider("openai"))
                .thenReturn(List.of(available, limited, inactive));

        GatewayStatusResponse status = statusService.getStatus();
        ProviderStatusResponse providerStatus = status.providers().get(0);

        assertThat(providerStatus.totalKeys()).isEqualTo(3);
        assertThat(providerStatus.availableKeys()).isEqualTo(1); // only 'available'
        assertThat(providerStatus.rateLimitedKeys()).isEqualTo(1);
        assertThat(providerStatus.inactiveKeys()).isEqualTo(1);
    }

    @Test
    void shouldMarkProviderAsAvailableWhenItHasActiveNonRateLimitedKeys() {
        when(providerRegistry.availableProviders()).thenReturn(List.of("openai"));
        when(apiKeyRepository.findAllByProvider("openai"))
                .thenReturn(List.of(new ApiKey("openai", "sk-good", 10, 60)));

        GatewayStatusResponse status = statusService.getStatus();

        assertThat(status.providers().get(0).available()).isTrue();
    }

    @Test
    void shouldMarkProviderAsUnavailableWhenNoUsableKeys() {
        when(providerRegistry.availableProviders()).thenReturn(List.of("openai"));

        ApiKey limited = new ApiKey("openai", "sk-limited", 1, 60);
        limited.recordRequest();

        when(apiKeyRepository.findAllByProvider("openai"))
                .thenReturn(List.of(limited));

        GatewayStatusResponse status = statusService.getStatus();

        assertThat(status.providers().get(0).available()).isFalse();
    }

    // ---------------------------------------------------------------
    // SECURITY — status must never expose key values
    // ---------------------------------------------------------------

    @Test
    void shouldNeverExposeKeyValuesInStatusResponse() {
        when(providerRegistry.availableProviders()).thenReturn(List.of("openai"));
        when(apiKeyRepository.findAllByProvider("openai"))
                .thenReturn(List.of(new ApiKey("openai", "sk-super-secret", 10, 60)));

        GatewayStatusResponse status = statusService.getStatus();

        // Serialize to string and verify key value never appears
        String serialized = status.toString();
        assertThat(serialized).doesNotContain("sk-super-secret");
    }
}