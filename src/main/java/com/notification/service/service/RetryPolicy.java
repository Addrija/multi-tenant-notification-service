package com.notification.service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

// attemptCount here means "attempts made so far, including the one that just failed."
// shouldRetry(5) with maxAttempts=5 is false - the 5th failure is the last one, no 6th try.
@Component
public class RetryPolicy {

    private final int maxAttempts;
    private final long baseBackoffSeconds;
    private final long maxBackoffSeconds;

    public RetryPolicy(@Value("${notification.retry.max-attempts}") int maxAttempts,
                        @Value("${notification.retry.base-backoff-seconds}") long baseBackoffSeconds,
                        @Value("${notification.retry.max-backoff-seconds}") long maxBackoffSeconds) {
        this.maxAttempts = maxAttempts;
        this.baseBackoffSeconds = baseBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    public boolean shouldRetry(int attemptCount) {
        return attemptCount < maxAttempts;
    }

    // Exponential backoff: base * 2^(attemptCount - 1) seconds after `now`, capped at
    // maxBackoffSeconds so growth flattens instead of exploding at high attempt counts.
    public Instant nextRetryAt(int attemptCount, Instant now) {
        long delaySeconds = Math.min(maxBackoffSeconds, baseBackoffSeconds * (1L << (attemptCount - 1)));
        return now.plusSeconds(delaySeconds);
    }
}