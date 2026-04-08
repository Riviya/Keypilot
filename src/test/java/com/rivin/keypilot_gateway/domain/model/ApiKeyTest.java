package com.rivin.keypilot_gateway.domain.model;


import com.rivin.keypilot_gateway.domain.exception.InvalidApiException;
import com.rivin.keypilot_gateway.domain.exception.InvalidApiProviderException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApiKeyTest {

    ////////////////////////////////// KeyValue Check //////////////////////////////////////
    @Test
    void shouldAcceptTheKeyIfValid() {
        ApiKey apiKey = new ApiKey("kjebwkf-ejfnkwjbf", "https://test.com");
        assertEquals("kjebwkf-ejfnkwjbf", apiKey.getKeyValue());
    }

    @Test
    void shouldRejectIfTheKeyIsNull(){
        InvalidApiException exception = assertThrows(InvalidApiException.class, () -> new ApiKey(null, "https://test.com"));
        assertEquals("API key cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectIfTheKeyIsBlank(){
        InvalidApiException exception = assertThrows(InvalidApiException.class, () -> new ApiKey("csd sdhs",  "https://test.com"));
        assertEquals("API key cannot be contains spaces", exception.getMessage());
    }


    ////////////////////////////////// Provider Check //////////////////////////////////////
    @Test
    void shouldAcceptTheProviderIfValid() {
        ApiKey apiKey = new ApiKey("kjebwkf-ejfnkwjbf", "https://test.com");
        assertEquals("https://test.com", apiKey.getProvider());
    }

    @Test
    void shouldRejectIfTheProviderIsNull(){
        InvalidApiProviderException exception = assertThrows(InvalidApiProviderException.class, () -> new ApiKey("kjebwkf-ejfnkwjbf", null));
        assertEquals("API key provider cannot be null or blank", exception.getMessage());
    }

    @Test
    void shouldRejectIfTheProviderIsBlank(){
        InvalidApiProviderException exception = assertThrows(InvalidApiProviderException.class, () -> new ApiKey("kjebwkf-ejfnkwjbf",  " https://test.com"));
        assertEquals("API key provider cannot contains spaces", exception.getMessage());

    }


    ////////////////////////////////// Deactivate Check //////////////////////////////////////
    @Test
    void shouldDeactivate(){
        ApiKey newKey = new ApiKey("kjebwkf-ejfnkwjbf", "https://test.com");
        assertEquals("API Key Deactivated!", newKey.deactivate());
    }

    @Test
    void shouldRejectIfTheKeyAlreadyDeactivated(){
        ApiKey newKey = new ApiKey("kjebwkf-ejfnkwjbf", "https://test.com");
        newKey.deactivate();
        assertEquals("API key has been already deactivated!", newKey.deactivate());
    }

    @Test
    void shouldHaveUniqueIds() {
        ApiKey key1 = new ApiKey("openai", "sk-test-1");
        ApiKey key2 = new ApiKey("openai", "sk-test-2");
        assertThat(key1.getId()).isNotEqualTo(key2.getId());
    }





}
