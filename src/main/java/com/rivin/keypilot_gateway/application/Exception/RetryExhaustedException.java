package com.rivin.keypilot_gateway.application.Exception;

public class RetryExhaustedException extends RuntimeException {

    private final String provider;
    private final int attempts;

    public RetryExhaustedException(String provider, int attempts) {
        super(String.format(
                "All %d retry attempts exhausted for provider [%s]. " +
                        "No available keys could serve the request.",
                attempts, provider
        ));
        this.provider = provider;
        this.attempts = attempts;
    }

    public String getProvider() { return provider; }
    public int getAttempts()    { return attempts; }
}