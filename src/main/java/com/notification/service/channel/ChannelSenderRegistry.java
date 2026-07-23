package com.notification.service.channel;

import com.notification.service.entity.enums.ChannelType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Spring auto-collects every ChannelSender bean into this List; built once into
// a lookup map keyed by ChannelType so the dispatcher can find the right sender
// for a notification without an if/switch chain.
@Component
public class ChannelSenderRegistry {

    private final Map<ChannelType, ChannelSender> sendersByType;

    public ChannelSenderRegistry(List<ChannelSender> senders) {
        this.sendersByType = senders.stream()
                .collect(Collectors.toUnmodifiableMap(ChannelSender::getChannelType, Function.identity()));
    }

    public ChannelSender getSender(ChannelType channelType) {
        ChannelSender sender = sendersByType.get(channelType);
        if (sender == null) {
            throw new IllegalStateException("No ChannelSender registered for channel type: " + channelType);
        }
        return sender;
    }
}