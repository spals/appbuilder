package net.spals.appbuilder.message.core.consumer;

import net.spals.appbuilder.config.message.MessageConsumerConfig;
import net.spals.appbuilder.message.core.formatter.MessageFormatter;

/**
 * @author tkral
 */
public interface MessageConsumerPlugin {

    void start(MessageConsumerConfig consumerConfig, MessageFormatter messageFormatter);

    void stop(MessageConsumerConfig consumerConfig);
}
