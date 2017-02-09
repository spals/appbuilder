package net.spals.appbuilder.message.core.consumer;

import net.spals.appbuilder.config.ConsumerConfig;

/**
 * @author tkral
 */
public interface MessageConsumerPlugin {

    void start(ConsumerConfig consumerConfig);

    void stop(ConsumerConfig consumerConfig);
}
