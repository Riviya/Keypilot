package com.rivin.keypilot_gateway.domain.rotation;

import com.rivin.keypilot_gateway.domain.model.ApiKey;
import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class RandomRotationStrategyTest {

    private RandomRotationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RandomRotationStrategy();
    }

    @Test
    void shouldReturnAnActiveKey() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "openai");

        ApiKey selected = strategy.selectKey(List.of(key1, key2));

        assertThat(selected).isIn(key1, key2);
        assertThat(selected.isActive()).isTrue();
    }

    @Test
    void shouldEventuallyReturnAllActiveKeys() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "openai");
        ApiKey key3 = new ApiKey("sk-3", "openai");
        List<ApiKey> keys = List.of(key1, key2, key3);

        Set<String> selectedIds = IntStream.range(0, 50)
                .mapToObj(i -> strategy.selectKey(keys).getId())
                .collect(Collectors.toSet());

        assertThat(selectedIds).containsExactlyInAnyOrder(
                key1.getId(), key2.getId(), key3.getId()
        );
    }

    @Test
    void shouldNeverSelectInactiveKey() {
        ApiKey active = new ApiKey("sk-1", "openai");
        ApiKey inactive = new ApiKey("sk-dead", "openai");
        inactive.deactivate();

        IntStream.range(0, 30).forEach(i -> {
            ApiKey selected = strategy.selectKey(List.of(active, inactive));
            assertThat(selected).isEqualTo(active);
        });
    }

    @Test
    void shouldThrowWhenListIsEmpty() {
        assertThatThrownBy(() -> strategy.selectKey(List.of()))
                .isInstanceOf(NoAvailableApiKeyException.class);
    }

    @Test
    void shouldThrowWhenAllKeysAreInactive() {
        ApiKey dead = new ApiKey("sk-dead", "openai");
        dead.deactivate();

        assertThatThrownBy(() -> strategy.selectKey(List.of(dead)))
                .isInstanceOf(NoAvailableApiKeyException.class);
    }
}