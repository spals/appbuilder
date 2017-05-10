package net.spals.appbuilder.message.core.consumer;

import net.spals.appbuilder.config.message.MessageConsumerConfig;
import net.spals.appbuilder.model.core.ModelSerializer;

/**
 * @author tkral
 */
public interface MessageConsumerPlugin {

    void start(MessageConsumerConfig consumerConfig, ModelSerializer modelSerializer);

    void stop(MessageConsumerConfig consumerConfig);
}
