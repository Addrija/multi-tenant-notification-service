package com.notification.service.entity;

import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.DeliveryOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Append-only log of each concrete ChannelSender.send() call and its outcome - the
// "retry attempts" half of the audit trail requirement, distinct from status transitions.
@Entity
@Table(name = "delivery_attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "channel_type")
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryOutcome outcome;

    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private Instant attemptedAt = Instant.now();
}