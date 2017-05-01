package net.spals.appbuilder.message.core.producer;

import net.spals.appbuilder.config.message.MessageProducerConfig;

import java.io.IOException;

/**
 * @author tkral
 */
public interface MessageProducerPlugin {

    void sendMessage(final MessageProducerConfig producerConfig, final byte[] serializedPayload) throws IOException;
}
