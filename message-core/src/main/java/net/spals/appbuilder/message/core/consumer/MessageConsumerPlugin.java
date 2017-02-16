package net.spals.appbuilder.message.core.consumer;

import net.spals.appbuilder.config.ConsumerConfig;
import net.spals.appbuilder.message.core.formatter.MessageFormatter;

/**
 * @author tkral
 */
public interface MessageConsumerPlugin {

    void start(ConsumerConfig consumerConfig, MessageFormatter messageFormatter);

    void stop(ConsumerConfig consumerConfig);
}
