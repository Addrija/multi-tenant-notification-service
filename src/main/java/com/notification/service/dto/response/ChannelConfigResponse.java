package com.notification.service.dto.response;

import com.notification.service.entity.ChannelConfig;
import com.notification.service.entity.enums.ChannelType;

public record ChannelConfigResponse(
        Long id,
        ChannelType channelType,
        boolean enabled,
        String senderIdentity,
        double simulatedFailureRate
) {
    public static ChannelConfigResponse from(ChannelConfig config) {
        return new ChannelConfigResponse(
                config.getId(),
                config.getChannelType(),
                config.isEnabled(),
                config.getSenderIdentity(),
                config.getSimulatedFailureRate());
    }
}