package com.rivin.keypilot_gateway.domain.rotation;


import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoundRobinRotationStrategyTest {

    @Test
    void shouldReturnKeysInRoundRobinOrder() {
        List<ApiKey> keys = List.of(
                new ApiKey("key-1", "https://test.com"),
                new ApiKey("key-2", "https://test.com"),
                new ApiKey("key-3", "https://test.com")
        );

        RoundRobinRotationStrategy strategy = new RoundRobinRotationStrategy();

        assertEquals("key-1", strategy.nextKey(keys).getKeyValue());
        assertEquals("key-2", strategy.nextKey(keys).getKeyValue());
        assertEquals("key-3", strategy.nextKey(keys).getKeyValue());
        assertEquals("key-1", strategy.nextKey(keys).getKeyValue());
    }

    @Test
    void shouldThrowExceptionWhenNoKeysAreAvailable() {

        assertThrows(IllegalArgumentException.class, () -> new RoundRobinRotationStrategy().nextKey(null));
    }

}
