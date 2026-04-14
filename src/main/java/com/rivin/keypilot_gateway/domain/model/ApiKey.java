package com.rivin.keypilot_gateway.domain.model;

import com.rivin.keypilot_gateway.domain.exception.InvalidApiException;
import com.rivin.keypilot_gateway.domain.exception.InvalidApiProviderException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiKey {

    private final String id;
    private final String provider;
    private final String keyValue;
    private boolean active;

    // Rate limiting state
    private final int maxRequestsPerWindow;
    private final long windowDurationSeconds;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private volatile Instant windowStart = Instant.now();

    // Default Constructor
    public ApiKey(String keyValue, String provider) {

        validate(keyValue, provider);

        this.id = UUID.randomUUID().toString();
        this.provider = provider;
        this.keyValue = keyValue;
        this.active = true;
        this.maxRequestsPerWindow = Integer.MAX_VALUE;
        this.windowDurationSeconds = 60;

    }

    public ApiKey(String keyValue, String provider, int maxRequestsPerWindow, long windowDurationSeconds) {

        validate(keyValue, provider);

        this.id = UUID.randomUUID().toString();
        this.provider = provider;
        this.keyValue = keyValue;
        this.active = true;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowDurationSeconds = windowDurationSeconds;
    }

    public ApiKey(String id, String provider, String keyValue,
                  boolean active, int maxRequestsPerWindow,
                  long windowDurationSeconds) {

        validate(keyValue, provider);

        this.id = id;
        this.provider = provider;
        this.keyValue = keyValue;
        this.active = active;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowDurationSeconds = windowDurationSeconds;
    }


    public String getId() { return id; }
    public String getKeyValue() { return keyValue; }
    public String getProvider() { return provider; }
    public boolean isActive() { return active; }
    public int getMaxRequestsPerWindow()   { return maxRequestsPerWindow; }
    public long getWindowDurationSeconds() { return windowDurationSeconds; }

    public boolean isRateLimited() {
        resetWindowIfExpired();
        return getRequestCount() >= maxRequestsPerWindow;
    }

    public int getRequestCount() {
        resetWindowIfExpired();
        return requestCount.get();
    }

    public void recordRequest() {
        resetWindowIfExpired();

        if (isRateLimited()) {
            return;
        }
        requestCount.incrementAndGet();
    }

    public void resetWindowIfExpired(){
        Instant now = Instant.now();
        Instant windowEnd = windowStart.plusSeconds(windowDurationSeconds);

        if (now.isAfter(windowEnd)) {
            // Reset window — volatile write ensures visibility across threads
            windowStart = now;
            requestCount.set(0);
        }

    }

    public String deactivate() {

        if (!isActive()) {
            return "API key has been already deactivated!";
        }
        this.active = false;
        return "API Key Deactivated!";
    }

    private void validate(String keyValue, String provider) {
        if (keyValue == null || keyValue.isBlank()) {
            throw new InvalidApiException("API key cannot be null or blank");
        }
        if (keyValue.contains(" ")) {
            throw new InvalidApiException("API key cannot contain spaces");
        }
        if (provider == null || provider.isBlank()) {
            throw new InvalidApiProviderException("Provider cannot be null or blank");
        }
        if (provider.contains(" ")) {
            throw new InvalidApiProviderException("Provider cannot contain spaces");
        }
    }

    // Thread safety
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiKey)) return false;
        return Objects.equals(id, ((ApiKey) o).id);
    }



}