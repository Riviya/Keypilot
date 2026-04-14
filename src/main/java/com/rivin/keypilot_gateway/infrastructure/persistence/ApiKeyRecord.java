package com.rivin.keypilot_gateway.infrastructure.persistence;

// Internal storage DTO — only used by FileBackedApiKeyRepository
// Package-private intentionally — nothing outside infrastructure should see this
record ApiKeyRecord(
        String id,
        String provider,
        String keyValue,
        boolean active,
        int maxRequestsPerWindow,
        long windowDurationSeconds
) {}