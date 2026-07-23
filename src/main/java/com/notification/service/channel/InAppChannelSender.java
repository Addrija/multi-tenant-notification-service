package com.notification.service.channel;

import com.notification.service.entity.enums.ChannelType;
import org.springframework.stereotype.Component;

// Routed through the same simulated interface as the other channels for
// architectural symmetry, even though a real in-app notification wouldn't have
// a transient-failure story - seed data sets its simulatedFailureRate to 0.0.
@Component
public class InAppChannelSender extends AbstractSimulatedChannelSender {
    @Override
    public ChannelType getChannelType() {
        return ChannelType.IN_APP;
    }
}