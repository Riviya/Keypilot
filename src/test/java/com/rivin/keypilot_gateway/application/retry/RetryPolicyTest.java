package com.rivin.keypilot_gateway.application.retry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RetryPolicyTest {

    // ---------------------------------------------------------------
    // CONSTRUCTION
    // ---------------------------------------------------------------

    @Test
    void shouldCreateWithValidMaxAttempts() {
        RetryPolicy policy = new RetryPolicy(3);
        assertThat(policy.maxAttempts()).isEqualTo(3);
    }

    @Test
    void shouldThrowWhenMaxAttemptsIsZero() {
        assertThatThrownBy(() -> new RetryPolicy(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxAttempts must be at least 1");
    }

    @Test
    void shouldThrowWhenMaxAttemptsIsNegative() {
        assertThatThrownBy(() -> new RetryPolicy(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------
    // RETRYABLE STATUS CODES
    // ---------------------------------------------------------------

    @Test
    void shouldConsider429Retryable() {
        RetryPolicy policy = new RetryPolicy(3);
        assertThat(policy.isRetryable(429)).isTrue();
    }

    @Test
    void shouldNotConsider200Retryable() {
        RetryPolicy policy = new RetryPolicy(3);
        assertThat(policy.isRetryable(200)).isFalse();
    }

    @Test
    void shouldNotConsider500Retryable() {
        RetryPolicy policy = new RetryPolicy(3);
        assertThat(policy.isRetryable(500)).isFalse();
    }

    @Test
    void shouldNotConsider400Retryable() {
        RetryPolicy policy = new RetryPolicy(3);
        assertThat(policy.isRetryable(400)).isFalse();
    }

    @Test
    void shouldNotConsider401Retryable() {
        RetryPolicy policy = new RetryPolicy(3);
        assertThat(policy.isRetryable(401)).isFalse();
    }

    @Test
    void shouldNotConsider404Retryable() {
        RetryPolicy policy = new RetryPolicy(3);
        assertThat(policy.isRetryable(404)).isFalse();
    }

    // ---------------------------------------------------------------
    // ATTEMPT TRACKING
    // ---------------------------------------------------------------

    @Test
    void shouldAllowAttemptsUpToMax() {
        RetryPolicy policy = new RetryPolicy(3);

        assertThat(policy.hasAttemptsRemaining(1)).isTrue(); // attempt 1 of 3
        assertThat(policy.hasAttemptsRemaining(2)).isTrue(); // attempt 2 of 3
        assertThat(policy.hasAttemptsRemaining(3)).isTrue(); // attempt 3 of 3
    }

    @Test
    void shouldNotAllowAttemptsExceedingMax() {
        RetryPolicy policy = new RetryPolicy(3);
        assertThat(policy.hasAttemptsRemaining(4)).isFalse();
    }
}