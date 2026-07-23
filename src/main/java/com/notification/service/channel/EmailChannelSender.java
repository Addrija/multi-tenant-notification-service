package com.notification.service.channel;

import com.notification.service.entity.enums.ChannelType;
import org.springframework.stereotype.Component;

@Component
public class EmailChannelSender extends AbstractSimulatedChannelSender {
    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }
}