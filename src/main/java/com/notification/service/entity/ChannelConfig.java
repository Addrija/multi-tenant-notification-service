package com.notification.service.entity;

import com.notification.service.entity.enums.ChannelType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// senderIdentity is just metadata (e.g. "from" email / sender id) - no real provider
// integration; simulatedFailureRate drives the ChannelSender's random success/failure.
@Entity
@Table(name = "channel_config", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "channel_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "channel_type")
    private ChannelType channelType;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    private String senderIdentity;

    @Column(nullable = false)
    @Builder.Default
    private double simulatedFailureRate = 0.1;
}