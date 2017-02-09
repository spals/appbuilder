package net.spals.appbuilder.message.core.consumer.callback;

import net.spals.appbuilder.message.core.model.Message;

/**
 * @author tkral
 */
public interface MessageConsumerCallback {

    void processMessage(Message message);
}
