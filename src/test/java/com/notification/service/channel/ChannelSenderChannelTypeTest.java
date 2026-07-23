package com.notification.service.channel;

import com.notification.service.entity.enums.ChannelType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Sanity check against copy/paste mistakes across the 4 near-identical subclasses.
class ChannelSenderChannelTypeTest {

    @Test
    void eachSenderReportsItsOwnChannelType() {
        assertThat(new EmailChannelSender().getChannelType()).isEqualTo(ChannelType.EMAIL);
        assertThat(new SmsChannelSender().getChannelType()).isEqualTo(ChannelType.SMS);
        assertThat(new PushChannelSender().getChannelType()).isEqualTo(ChannelType.PUSH);
        assertThat(new InAppChannelSender().getChannelType()).isEqualTo(ChannelType.IN_APP);
    }
}