package com.notification.service.channel;

import com.notification.service.entity.ChannelConfig;
import com.notification.service.entity.enums.ChannelType;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractSimulatedChannelSenderTest {

    private static class TestChannelSender extends AbstractSimulatedChannelSender {
        TestChannelSender(Random random) {
            super(random);
        }

        @Override
        public ChannelType getChannelType() {
            return ChannelType.EMAIL;
        }
    }

    @Test
    void succeedsWhenRollIsAboveFailureRate() {
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.5);
        ChannelConfig config = ChannelConfig.builder().simulatedFailureRate(0.1).build();

        SendResult result = new TestChannelSender(random).send("user@example.com", "hi", config);

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void failsWhenRollIsBelowFailureRate() {
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.05);
        ChannelConfig config = ChannelConfig.builder().simulatedFailureRate(0.1).build();

        SendResult result = new TestChannelSender(random).send("user@example.com", "hi", config);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    void neverFailsWhenFailureRateIsZero() {
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.0);
        ChannelConfig config = ChannelConfig.builder().simulatedFailureRate(0.0).build();

        SendResult result = new TestChannelSender(random).send("user@example.com", "hi", config);

        assertThat(result.success()).isTrue();
    }
}