package net.spals.appbuilder.message.core.consumer;

import com.google.inject.Inject;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.config.message.MessageConsumerConfig;
import net.spals.appbuilder.model.core.ModelSerializer;

import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindSingleton(baseClass = MessageConsumer.class)
class DefaultMessageConsumer implements MessageConsumer {

    private final Map<String, MessageConsumerConfig> consumerConfigMap;
    private final Map<String, ModelSerializer> serializerMap;

    private final Map<String, MessageConsumerPlugin> consumerPluginMap;

    @Inject
    DefaultMessageConsumer(final Map<String, MessageConsumerConfig> consumerConfigMap,
                           final Map<String, ModelSerializer> serializerMap,
                           final Map<String, MessageConsumerPlugin> consumerPluginMap) {
        this.consumerConfigMap = consumerConfigMap;

        this.serializerMap = serializerMap;
        this.consumerPluginMap = consumerPluginMap;
    }

    @Override
    public void start() {
        consumerConfigMap.entrySet().stream()
            .filter(consumerConfigEntry -> consumerConfigEntry.getValue().isActive())
            .forEach(consumerConfigEntry -> {
                final String tag = consumerConfigEntry.getKey();
                final MessageConsumerConfig consumerConfig = consumerConfigEntry.getValue();

                final MessageConsumerPlugin consumerPlugin = Optional.ofNullable(consumerPluginMap.get(consumerConfig.getSource()))
                    .orElseThrow(() -> new ConfigException.BadValue(tag + ".consumer.source",
                        "No message consumer plugin found for source: " + consumerConfig.getSource()));

                final ModelSerializer serializer = Optional.ofNullable(serializerMap.get(consumerConfig.getFormat()))
                    .orElseThrow(() -> new ConfigException.BadValue(tag + ".consumer.format",
                        "No message formatter plugin found for format: " + consumerConfig.getFormat()));

                consumerPlugin.start(consumerConfig, serializer);
            });
    }

    @Override
    public void stop() {
        consumerConfigMap.entrySet().stream()
            .filter(consumerConfigEntry -> consumerConfigEntry.getValue().isActive())
            .forEach(consumerConfigEntry -> {
                final MessageConsumerConfig consumerConfig = consumerConfigEntry.getValue();
                // Assuming start() is called before stop() so these are guaranteed to be non-null
                final MessageConsumerPlugin consumerPlugin = consumerPluginMap.get(consumerConfig.getSource());

                consumerPlugin.stop(consumerConfig);
            });
    }
}
