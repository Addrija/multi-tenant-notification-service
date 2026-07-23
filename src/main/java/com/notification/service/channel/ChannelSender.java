package com.notification.service.channel;

import com.notification.service.entity.ChannelConfig;
import com.notification.service.entity.enums.ChannelType;

// One implementation per ChannelType, simulated (no real provider integration -
// out of scope per assignment). The orchestrator picks the implementation whose
// getChannelType() matches a notification's channelType.
public interface ChannelSender {
    ChannelType getChannelType();

    SendResult send(String recipient, String renderedContent, ChannelConfig channelConfig);
}