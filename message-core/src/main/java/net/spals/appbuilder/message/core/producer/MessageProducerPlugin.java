package net.spals.appbuilder.message.core.producer;

import net.spals.appbuilder.message.core.model.Message;

/**
 * @author tkral
 */
public interface MessageProducerPlugin {

    void sendMessage(final Message message);
}
