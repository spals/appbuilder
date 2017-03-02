package net.spals.appbuilder.message.core.producer;

import java.util.Map;

/**
 * @author tkral
 */
public interface MessageProducer {

    void sendMessage(String tag, Map<String, Object> payload);

}
