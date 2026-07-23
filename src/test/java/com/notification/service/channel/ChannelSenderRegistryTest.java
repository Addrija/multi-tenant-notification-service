package com.notification.service.channel;

import com.notification.service.entity.enums.ChannelType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelSenderRegistryTest {

    private final ChannelSenderRegistry registry = new ChannelSenderRegistry(List.of(
            new EmailChannelSender(), new SmsChannelSender(), new PushChannelSender(), new InAppChannelSender()));

    @Test
    void resolvesEachChannelTypeToTheMatchingSender() {
        assertThat(registry.getSender(ChannelType.EMAIL)).isInstanceOf(EmailChannelSender.class);
        assertThat(registry.getSender(ChannelType.SMS)).isInstanceOf(SmsChannelSender.class);
        assertThat(registry.getSender(ChannelType.PUSH)).isInstanceOf(PushChannelSender.class);
        assertThat(registry.getSender(ChannelType.IN_APP)).isInstanceOf(InAppChannelSender.class);
    }

    @Test
    void throwsWhenNoSenderRegisteredForAChannelType() {
        ChannelSenderRegistry emptyRegistry = new ChannelSenderRegistry(List.of());

        assertThatThrownBy(() -> emptyRegistry.getSender(ChannelType.EMAIL))
                .isInstanceOf(IllegalStateException.class);
    }
}