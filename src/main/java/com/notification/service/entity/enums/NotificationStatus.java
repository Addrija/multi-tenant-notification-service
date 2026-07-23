package com.notification.service.entity.enums;

/**
 * PENDING: created, not yet claimed by the poller (waiting on scheduledAt if a scheduled send).
 * IN_PROGRESS: claimed by the poller, a worker thread is actively dispatching it.
 * RETRYING: an attempt failed, retries remain; waits until nextRetryAt before re-claim.
 * SENT: terminal, success.
 * FAILED: terminal, retries exhausted (permanent failure) - not the same as a single failed attempt.
 */
public enum NotificationStatus {
    PENDING,
    IN_PROGRESS,
    RETRYING,
    SENT,
    FAILED
}