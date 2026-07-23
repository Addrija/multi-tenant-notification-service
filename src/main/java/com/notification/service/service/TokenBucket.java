package com.notification.service.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// Lazy time-based refill: no background thread, tokens are topped up based on
// elapsed time whenever tryConsume() is called. Thread-safe via synchronized -
// contention is expected to be low (one bucket per tenant).
public class TokenBucket {

    private final double capacity;
    private final double refillRatePerSec;
    private final Clock clock;

    private double tokens;
    private Instant lastRefill;

    public TokenBucket(double capacity, double refillRatePerSec, Clock clock) {
        this.capacity = capacity;
        this.refillRatePerSec = refillRatePerSec;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefill = clock.instant();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        Instant now = clock.instant();
        double elapsedSeconds = Duration.between(lastRefill, now).toNanos() / 1_000_000_000.0;
        if (elapsedSeconds > 0) {
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillRatePerSec);
            lastRefill = now;
        }
    }
}