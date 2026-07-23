package com.notification.service.channel;

import com.notification.service.entity.ChannelConfig;

import java.util.Random;

// Shared simulation logic for all channels: roll a random number, fail if it lands
// under the tenant's configured simulatedFailureRate for this channel. Random is
// constructor-injectable so tests can pass a seeded/mocked instance for determinism.
public abstract class AbstractSimulatedChannelSender implements ChannelSender {

    private final Random random;

    protected AbstractSimulatedChannelSender() {
        this(new Random());
    }

    protected AbstractSimulatedChannelSender(Random random) {
        this.random = random;
    }

    @Override
    public SendResult send(String recipient, String renderedContent, ChannelConfig channelConfig) {
        double roll = random.nextDouble();
        if (roll < channelConfig.getSimulatedFailureRate()) {
            return SendResult.failure(
                    "Simulated " + getChannelType() + " delivery failure to " + recipient);
        }
        return SendResult.ok();
    }
}