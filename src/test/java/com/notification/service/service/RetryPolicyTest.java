package com.notification.service.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    private final RetryPolicy retryPolicy = new RetryPolicy(5, 10, 300);

    @Test
    void allowsRetryUntilMaxAttemptsReached() {
        assertThat(retryPolicy.shouldRetry(1)).isTrue();
        assertThat(retryPolicy.shouldRetry(4)).isTrue();
        assertThat(retryPolicy.shouldRetry(5)).isFalse();
        assertThat(retryPolicy.shouldRetry(6)).isFalse();
    }

    @Test
    void backoffDoublesEachAttempt() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        assertThat(retryPolicy.nextRetryAt(1, now)).isEqualTo(now.plusSeconds(10));
        assertThat(retryPolicy.nextRetryAt(2, now)).isEqualTo(now.plusSeconds(20));
        assertThat(retryPolicy.nextRetryAt(3, now)).isEqualTo(now.plusSeconds(40));
        assertThat(retryPolicy.nextRetryAt(4, now)).isEqualTo(now.plusSeconds(80));
    }

    @Test
    void backoffIsCappedAtMaxBackoffSeconds() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        RetryPolicy policyWithManyAttempts = new RetryPolicy(20, 10, 300);

        // Uncapped this would be 10 * 2^9 = 5120s - should clamp to the 300s cap instead.
        assertThat(policyWithManyAttempts.nextRetryAt(10, now)).isEqualTo(now.plusSeconds(300));
    }
}