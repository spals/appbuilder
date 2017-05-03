package net.spals.appbuilder.message.core.producer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.config.message.MessageProducerConfig;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindSingleton(baseClass = MessageProducer.class)
class DefaultMessageProducer implements MessageProducer {

    private final Map<String, MessageProducerConfig> producerConfigMap;
    private final Map<String, ModelSerializer> serializerMap;

    private final Map<String, MessageProducerPlugin> producerPluginMap;

    @Inject
    DefaultMessageProducer(final Map<String, MessageProducerConfig> producerConfigMap,
                           final Map<String, ModelSerializer> serializerMap,
                           final Map<String, MessageProducerPlugin> producerPluginMap) {
        this.producerConfigMap = producerConfigMap;

        this.serializerMap = serializerMap;
        this.producerPluginMap = producerPluginMap;
    }

    @Override
    public void sendMessage(final String tag, final Object payload) {
        Preconditions.checkNotNull(tag, "Cannot send Message with null tag");
        Preconditions.checkNotNull(payload, "Cannot send Message with null payload");

        final MessageProducerConfig producerConfig = loadProducerConfig(tag);
        final Logger logger = loadLogger(producerConfig);
        final ModelSerializer modelSerializer = loadModelSerializer(producerConfig);
        final MessageProducerPlugin messageProducerPlugin = loadMessageProducerPlugin(producerConfig);

        final byte[] serializedPayload;
        try {
            serializedPayload = modelSerializer.serialize(payload);
        } catch (IOException e) {
            logger.error("Error while serializing message payload with " + producerConfig.getFormat(), e);
            return;
        }

        try {
            messageProducerPlugin.sendMessage(producerConfig, serializedPayload);
        } catch (IOException e) {
            logger.error("Error while sending message to " + producerConfig.getDestination(), e);
            return;
        }
    }

    Logger loadLogger(final MessageProducerConfig producerConfig) {
        return LoggerFactory.getLogger(DefaultMessageProducer.class.getName() + "[" + producerConfig.getTag() + "]");
    }

    ModelSerializer loadModelSerializer(final MessageProducerConfig producerConfig) {
        return Optional.ofNullable(serializerMap.get(producerConfig.getFormat()))
                .orElseThrow(() -> new ConfigException.BadValue(producerConfig.getTag() + ".producer.format",
                        "No model serializer plugin found for format: " + producerConfig.getFormat()));
    }

    MessageProducerPlugin loadMessageProducerPlugin(final MessageProducerConfig producerConfig) {
        return Optional.ofNullable(producerPluginMap.get(producerConfig.getDestination()))
                .orElseThrow(() -> new ConfigException.BadValue(producerConfig.getTag() + ".producer.destination",
                        "No message producer plugin found for destination: " + producerConfig.getDestination()));
    }

    MessageProducerConfig loadProducerConfig(final String tag) {
        return Optional.ofNullable(producerConfigMap.get(tag))
                .orElseThrow(() -> new IllegalArgumentException("No MessageProducerConfig found for tag '" + tag + "'"));
    }
}
