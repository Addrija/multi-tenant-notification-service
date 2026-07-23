package com.notification.service.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketTest {

    @Test
    void startsFullAndAllowsUpToCapacityConsumes() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Clock clock = Clock.fixed(t0, ZoneOffset.UTC);
        TokenBucket bucket = new TokenBucket(3, 1.0, clock);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void refillsBasedOnElapsedTime() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(t0);
        TokenBucket bucket = new TokenBucket(2, 1.0, clock);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();

        clock.advanceSeconds(1);
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void refillNeverExceedsCapacity() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        MutableClock clock = new MutableClock(t0);
        TokenBucket bucket = new TokenBucket(2, 1.0, clock);

        clock.advanceSeconds(100);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}