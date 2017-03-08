package net.spals.appbuilder.message.core.consumer;

import net.spals.appbuilder.config.message.MessageConsumerConfig;

import java.util.Map;

/**
 * @author tkral
 */
public interface MessageConsumerCallback {

    void processMessage(MessageConsumerConfig consumerConfig, Map<String, Object> payload);
}
