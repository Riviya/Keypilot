package com.rivin.keypilot_gateway.application.retry;


public class RetryPolicy {

    private final int maxAttempts;
    private static final int RATE_LIMITED_STATUS = 429;

    public RetryPolicy(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "maxAttempts must be at least 1, got: " + maxAttempts
            );
        }
        this.maxAttempts = maxAttempts;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean isRetryable(int httpStatusCode) {
        return httpStatusCode == RATE_LIMITED_STATUS;
    }

    public boolean hasAttemptsRemaining(int attemptNumber) {
        return attemptNumber <= maxAttempts;
    }
}