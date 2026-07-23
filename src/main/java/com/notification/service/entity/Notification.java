package com.notification.service.entity;

import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// status is a state machine - see NotificationStatus for transitions/meaning.
// version backs optimistic-lock claiming in the poller, preventing double-dispatch
// when a concurrent claim races (loser gets OptimisticLockException, skips).
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "channel_type")
    private ChannelType channelType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @Column(nullable = false)
    private String recipient;

    // Raw variables map supplied by the caller, stored as JSON text for audit purposes.
    @Lob
    private String variables;

    @Lob
    private String renderedContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    private Instant scheduledAt;

    @Column(nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    private Instant nextRetryAt;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}