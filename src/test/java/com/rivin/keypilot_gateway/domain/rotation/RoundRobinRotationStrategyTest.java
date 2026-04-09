package com.rivin.keypilot_gateway.domain.rotation;

import com.rivin.keypilot_gateway.domain.exception.NoAvailableApiKeyException;
import com.rivin.keypilot_gateway.domain.model.ApiKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RoundRobinRotationStrategyTest {

    private RoundRobinRotationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RoundRobinRotationStrategy();
    }

    @Test
    void shouldReturnFirstKeyOnFirstCall() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "openai");

        ApiKey selected = strategy.selectKey(List.of(key1, key2));

        assertThat(selected).isEqualTo(key1);
    }

    @Test
    void shouldReturnSecondKeyOnSecondCall() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "openai");
        List<ApiKey> keys = List.of(key1, key2);

        strategy.selectKey(keys);
        ApiKey selected = strategy.selectKey(keys);

        assertThat(selected).isEqualTo(key2);
    }

    @Test
    void shouldWrapAroundAfterLastKey() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "openai");
        List<ApiKey> keys = List.of(key1, key2);

        strategy.selectKey(keys); // → key1
        strategy.selectKey(keys); // → key2
        ApiKey selected = strategy.selectKey(keys); // → wraps to key1

        assertThat(selected).isEqualTo(key1);
    }

    @Test
    void shouldReturnSingleKeyRepeatedly() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        List<ApiKey> keys = List.of(key1);

        assertThat(strategy.selectKey(keys)).isEqualTo(key1);
        assertThat(strategy.selectKey(keys)).isEqualTo(key1);
    }

    @Test
    void shouldSkipInactiveKeys() {
        ApiKey active = new ApiKey("sk-1", "openai");
        ApiKey inactive = new ApiKey("sk-dead", "openai");
        inactive.deactivate();

        ApiKey selected = strategy.selectKey(List.of(inactive, active));

        assertThat(selected).isEqualTo(active);
    }

    @Test
    void shouldOnlyRotateThroughActiveKeys() {
        ApiKey key1 = new ApiKey("sk-1", "openai");
        ApiKey key2 = new ApiKey("sk-2", "openai");
        ApiKey inactive = new ApiKey("sk-dead", "openai");
        inactive.deactivate();

        List<ApiKey> keys = List.of(key1, inactive, key2);

        ApiKey first  = strategy.selectKey(keys);
        ApiKey second = strategy.selectKey(keys);
        ApiKey third  = strategy.selectKey(keys); // wraps, must skip inactive

        assertThat(first).isEqualTo(key1);
        assertThat(second).isEqualTo(key2);
        assertThat(third).isEqualTo(key1);
    }

    @Test
    void shouldThrowWhenListIsEmpty() {
        assertThatThrownBy(() -> strategy.selectKey(List.of()))
                .isInstanceOf(NoAvailableApiKeyException.class)
                .hasMessageContaining("No active API keys available");
    }

    @Test
    void shouldThrowWhenAllKeysAreInactive() {
        ApiKey dead1 = new ApiKey("sk-dead2", "openai");
        ApiKey dead2 = new ApiKey("sk-dead1", "openai");
        dead1.deactivate();
        dead2.deactivate();

        assertThatThrownBy(() -> strategy.selectKey(List.of(dead1, dead2)))
                .isInstanceOf(NoAvailableApiKeyException.class);
    }
}