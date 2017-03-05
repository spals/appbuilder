package net.spals.appbuilder.message.core.producer;

import net.spals.appbuilder.config.ProducerConfig;

import java.io.IOException;

/**
 * @author tkral
 */
public interface MessageProducerPlugin {

    void sendMessage(final ProducerConfig producerConfig, final byte[] serializedPayload) throws IOException;
}
