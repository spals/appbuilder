package net.spals.appbuilder.message.core.producer;

import java.util.Map;

/**
 * @author tkral
 */
public interface MessageProducer {

    void sendMessage(final Map<String, Object> payload);

}
