package com.rivin.keypilot_gateway.application.service;

import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.exception.InvalidApiException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RateLimiterServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    public void setup() {
        rateLimiterService = new RateLimiterService(apiKeyRepository);
    }

    ////////////////////////////////// Recording Usage Check //////////////////////////////////////
    @Test
    void shouldRecordRequestOnKey() {

        // Arrange
        ApiKey apiKey = new ApiKey("sk-test", "openai", 2, 10);
        String apiKeyId = apiKey.getId();
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        // Act
        rateLimiterService.recordUsage(apiKeyId);

        // Assert
        assertThat(apiKey.getRequestCount()).isEqualTo(1);

    }

    @Test
    void shouldThrowWhenRecordingUsageForUnknownKey() {

        // Arrange
        ApiKey apiKey = new ApiKey("sk-test", "openai", 2, 10);
        when(apiKeyRepository.findById(apiKey.getId())).thenReturn(Optional.empty());

        // Act & Assert
        InvalidApiException exception = assertThrows(InvalidApiException.class, () -> rateLimiterService.recordUsage(apiKey.getId()));
        assertThat(exception.getMessage()).isEqualTo("No api key found with id: " + apiKey.getId());
    }

    ////////////////////////////////// Available Key Check //////////////////////////////////////
    @Test
    void shouldReturnOnlyNonRateLimitedKeys() {

        // Arrange
        ApiKey apiKey1 = new ApiKey("sk-test1", "openai", 2, 10);
        ApiKey apiKey2 = new ApiKey("sk-test2", "openai", 2, 10);
        ApiKey apiKey3 = new ApiKey("sk-test3", "openai", 1, 10);
        when(apiKeyRepository.findById(apiKey1.getId())).thenReturn(Optional.of(apiKey1));
        when(apiKeyRepository.findById(apiKey3.getId())).thenReturn(Optional.of(apiKey3));
        when(apiKeyRepository.findAllByProvider("openai")).thenReturn(List.of(apiKey1, apiKey2, apiKey3));

        // Act
        rateLimiterService.recordUsage(apiKey1.getId());
        rateLimiterService.recordUsage(apiKey3.getId());  // Now Should be rate limited
        List<ApiKey> result = rateLimiterService.getAvailableKeys("openai");

        // Assert
        assertThat(result).doesNotMatch(apiKey3::equals);
        assertThat(result).isEqualTo(List.of(apiKey1, apiKey2));

    }

    @Test
    void shouldReturnEmptyListWhenAllKeysRateLimited(){

        // Arrange
        ApiKey apiKey1 = new ApiKey("sk-test1", "openai", 1, 10);
        ApiKey apiKey2 = new ApiKey("sk-test2", "openai", 1, 10);

        when(apiKeyRepository.findById(apiKey1.getId())).thenReturn(Optional.of(apiKey1));
        when(apiKeyRepository.findById(apiKey2.getId())).thenReturn(Optional.of(apiKey2));
        when(apiKeyRepository.findAllByProvider("openai")).thenReturn(List.of(apiKey1, apiKey2));


        // Act
        rateLimiterService.recordUsage(apiKey1.getId());  // Rate Limited
        rateLimiterService.recordUsage(apiKey2.getId());  // Rate Limited
        List <ApiKey> result = rateLimiterService.getAvailableKeys("openai");

        // Assert
        assertThat(result).isEqualTo(List.of());
    }

    @Test
    void shouldIncludeActiveNonRateLimitedKeysOnly(){
        // Arrange
        ApiKey apiKey1 = new ApiKey("sk-test1", "openai", 3, 10);
        ApiKey apiKey2 = new ApiKey("sk-test1", "openai", 1, 10);
        ApiKey apiKey3 = new ApiKey("sk-test1", "openai", 1, 10);
        ApiKey apiKey4 = new ApiKey("sk-test1", "openai", 1, 10);

        when(apiKeyRepository.findById(apiKey2.getId())).thenReturn(Optional.of(apiKey2));
        when(apiKeyRepository.findById(apiKey1.getId())).thenReturn(Optional.of(apiKey1));
        when(apiKeyRepository.findAllByProvider("openai")).thenReturn(List.of(apiKey1, apiKey2,  apiKey3, apiKey4));
        apiKey4.deactivate();    // apiKey4 is not activated now

        // Act
        rateLimiterService.recordUsage(apiKey2.getId());     // apiKey2 is rate limited now
        rateLimiterService.recordUsage(apiKey1.getId());
        List<ApiKey> result = rateLimiterService.getAvailableKeys("openai");

        // Assert
        assertThat(result).isNotEqualTo(List.of(apiKey4, apiKey2));
        assertThat(result).isEqualTo(List.of(apiKey1, apiKey3));


    }

    @Test
    void shouldMarkKeyAsImmediatelyRateLimited() {
        ApiKey key = new ApiKey("openai", "sk-test", 100, 60);
        String keyId = key.getId();
        when(apiKeyRepository.findById(keyId)).thenReturn(java.util.Optional.of(key));

        rateLimiterService.markAsRateLimited(keyId);

        // Key must now report as rate limited regardless of request count
        assertThat(key.isRateLimited()).isTrue();
    }

    @Test
    void shouldThrowWhenMarkingUnknownKeyAsRateLimited() {
        when(apiKeyRepository.findById("ghost")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> rateLimiterService.markAsRateLimited("ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }


}
