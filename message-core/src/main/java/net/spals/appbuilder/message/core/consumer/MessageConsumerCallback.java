package net.spals.appbuilder.message.core.consumer;

import net.spals.appbuilder.config.ConsumerConfig;

import java.util.Map;

/**
 * @author tkral
 */
public interface MessageConsumerCallback {

    void processMessage(ConsumerConfig consumerConfig, Map<String, Object> payload);
}
