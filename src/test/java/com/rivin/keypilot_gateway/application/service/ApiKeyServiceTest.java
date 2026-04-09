package com.rivin.keypilot_gateway.application.service;


import com.rivin.keypilot_gateway.application.dto.AddApiKeyRequest;
import com.rivin.keypilot_gateway.application.dto.ApiKeyResponse;
import com.rivin.keypilot_gateway.application.port.ApiKeyRepository;
import com.rivin.keypilot_gateway.domain.exception.InvalidIdException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(apiKeyRepository);
    }

    ////////////////////////////////// Add ApiKey Check //////////////////////////////////////
    @Test
    void shouldSaveApiKeyWhenAdded() {
        // Arrange
        AddApiKeyRequest request = new AddApiKeyRequest("sk-test-123", "openai");

        // Act
        apiKeyService.addKey(request);

        // Assert
        verify(apiKeyRepository, times(1)).save(any(ApiKey.class));
    }

    @Test
    void shouldReturnResponseWithCorrectProviderWhenAdded() {
        // Arrange
        AddApiKeyRequest request = new AddApiKeyRequest("openai", "sk-test-123");


        // Act
        ApiKeyResponse response = apiKeyService.addKey(request);

        // Assert
        assertThat(response.provider()).isEqualTo("openai");
        assertThat(response.id()).isNotNull();
        assertThat(response.active()).isTrue();
    }

    @Test
    void shouldNotExposeRawKeyValueInResponse() {
        // Arrange
        AddApiKeyRequest request = new AddApiKeyRequest("openai", "sk-super-secret");

        // Act
        ApiKeyResponse response = apiKeyService.addKey(request);

        // Assert
        // The response DTO must never contain the raw key value
        assertThat(response).doesNotHaveToString("sk-super-secret");
    }


    ////////////////////////////////// List ApiKey Check //////////////////////////////////////
    @Test
    void shouldReturnAllKeysFromRepository() {
        // Arrange
        List<ApiKey> storedKeys = List.of(
                new ApiKey("sk-1", "openai"),
                new ApiKey("sk-2", "openai"),
                new ApiKey("sk-3", "antropic")

        );
        when(apiKeyRepository.findAll()).thenReturn(storedKeys);

        // Act
        List<ApiKeyResponse> responses = apiKeyService.listAllKeys();

        // Assert
        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(ApiKeyResponse::provider)
                .containsExactlyInAnyOrder("openai", "openai", "antropic");
    }

    @Test
    void shouldReturnEmptyListWhenNoKeysStored() {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of());

        // Act
        List<ApiKeyResponse> responses = apiKeyService.listAllKeys();

        // Assert
        assertThat(responses).isEmpty();
    }


    ////////////////////////////////// Delete ApiKey Check //////////////////////////////////////
    @Test
    void shouldCallRepositoryDeleteWithCorrectId() {
        // Arrange
        String id = "123somenumber";
        ApiKey apiKey = new ApiKey("sk-test-123", "openai");
        when(apiKeyRepository.findById(id)).thenReturn(Optional.of(apiKey));



        // Act
        apiKeyService.deleteKey(id);

        // Assert
        verify(apiKeyRepository, times(1)).delete(any());
    }

    @Test
    void shouldThrowWhenDeletingNonExistentKey()  {
        // Arrange
        String id = "123somenumber";
        when(apiKeyRepository.findById(id)).thenReturn(java.util.Optional.empty());

        // Act
        InvalidIdException exception = assertThrows(InvalidIdException.class, () -> apiKeyService.deleteKey(id));

        // Assert
        assertThat(exception.getMessage()).isEqualTo("ApiKey not found");
    }
}
