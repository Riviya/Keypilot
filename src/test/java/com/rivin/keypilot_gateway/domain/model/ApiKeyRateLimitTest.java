package com.rivin.keypilot_gateway.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApiKeyRateLimitTest {

    ////////////////////////////////// Initial State Check //////////////////////////////////////
    @Test
    void shouldNotBeRateLimitedOnCreation(){
        ApiKey apikey = new ApiKey("sk-key", "openai", 12, 50);
        assertThat(apikey.isRateLimited()).isFalse();
    }

    @Test
    void shouldHaveZeroRequestCountOnCreation(){
        ApiKey apikey = new ApiKey("sk-key", "openai", 10,50);
        assertThat(apikey.getRequestCount()).isEqualTo(0);
    }

    ////////////////////////////////// Request Tracking Check //////////////////////////////////////
    @Test
    void shouldIncrementRequestCountOnEachUse(){
        ApiKey apikey = new ApiKey("sk-key", "openai", 10,50);

        apikey.recordRequest();
        apikey.recordRequest();
        apikey.recordRequest();

        assertThat(apikey.getRequestCount()).isEqualTo(3);
    }

    @Test
    void shouldNotBeRateLimitedBelowThreshold() {
        ApiKey apikey = new ApiKey("sk-key", "openai", 4, 50);

        apikey.recordRequest();
        apikey.recordRequest();
        apikey.recordRequest();
        apikey.recordRequest();

        assertThat(apikey.isRateLimited()).isFalse();
    }

    @Test
    void shouldBecomeRateLimitedWhenThresholdReached() {
        ApiKey apikey = new ApiKey("sk-key", "openai", 3, 50);

        apikey.recordRequest();
        apikey.recordRequest();
        apikey.recordRequest();
        apikey.recordRequest();

        assertThat(apikey.isRateLimited()).isTrue();
    }

    @Test
    void shouldRemainRateLimitedAfterExceedingThreshold() {
        ApiKey apikey = new ApiKey("sk-key", "openai", 2, 50);

        apikey.recordRequest();
        apikey.recordRequest();
        apikey.recordRequest(); // should stop
        apikey.recordRequest();

        assertThat(apikey.isRateLimited()).isTrue();
        assertThat(apikey.getRequestCount()).isEqualTo(2);
    }

    ////////////////////////////////// Window Reset Check //////////////////////////////////////
    @Test
    void shouldResetAfterWindowExpires() throws InterruptedException {
        ApiKey key = new ApiKey("openai", "sk-test", 2, 1);

        key.recordRequest();
        key.recordRequest(); // rate limited

        assertThat(key.isRateLimited()).isTrue();

        Thread.sleep(1100); // wait for window to expire

        assertThat(key.isRateLimited()).isFalse();
        assertThat(key.getRequestCount()).isZero();
    }

    @Test
    void shouldAllowRequestsAgainAfterWindowReset() throws InterruptedException {
        ApiKey key = new ApiKey("openai", "sk-test", 2, 1);

        key.recordRequest();
        key.recordRequest(); // rate limited

        assertThat(key.isRateLimited()).isTrue();

        Thread.sleep(1100); // wait for window to expire

        key.recordRequest(); // after window reset

        assertThat(key.isRateLimited()).isFalse();
        assertThat(key.getRequestCount()).isEqualTo(1);
    }

    ////////////////////////////////// Thread Safety Check //////////////////////////////////////

    @Test
    void shouldHandleConcurrentRequestRecordingWithoutCorruption()
            throws InterruptedException {
        ApiKey key = new ApiKey("openai", "sk-test", 1000, 60);
        int threadCount = 50;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(key::recordRequest);
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // With atomic operations, count must be exactly threadCount
        assertThat(key.getRequestCount()).isEqualTo(threadCount);
    }
}
