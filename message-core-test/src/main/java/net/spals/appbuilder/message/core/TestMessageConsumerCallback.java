package net.spals.appbuilder.message.core;

import net.spals.appbuilder.config.message.MessageConsumerConfig;

import java.util.concurrent.CountDownLatch;

/**
 * A {@link MessageConsumerCallback} to be used for testing purposes.
 *
 * @author tkral
 */
public class TestMessageConsumerCallback implements MessageConsumerCallback<String> {

    private final CountDownLatch cdl;
    private final String expectedPayload;

    public TestMessageConsumerCallback(final CountDownLatch cdl,
                                       final String expectedPayload) {
        this.cdl = cdl;
        this.expectedPayload = expectedPayload;
    }

    @Override
    public String getTag() {
        return "myTag";
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @Override
    public void processMessage(final MessageConsumerConfig consumerConfig, final String payload) {
        assert payload.equals(expectedPayload);
        cdl.countDown();
    }
}