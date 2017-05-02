package net.spals.appbuilder.message.core.producer;

import com.google.inject.Inject;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.config.message.MessageProducerConfig;
import net.spals.appbuilder.message.core.formatter.MessageFormatter;
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
    private final Map<String, MessageFormatter> formatterMap;

    private final Map<String, MessageProducerPlugin> producerPluginMap;

    @Inject
    DefaultMessageProducer(final Map<String, MessageProducerConfig> producerConfigMap,
                           final Map<String, MessageFormatter> formatterMap,
                           final Map<String, MessageProducerPlugin> producerPluginMap) {
        this.producerConfigMap = producerConfigMap;

        this.formatterMap = formatterMap;
        this.producerPluginMap = producerPluginMap;
    }

    @Override
    public void sendMessage(final String tag, final Object payload) {
        final MessageProducerConfig producerConfig = loadProducerConfig(tag);
        final Logger logger = loadLogger(producerConfig);
        final MessageFormatter messageFormatter = loadMessageFormatter(producerConfig);
        final MessageProducerPlugin messageProducerPlugin = loadMessageProducerPlugin(producerConfig);

        final byte[] serializedPayload;
        try {
            serializedPayload = messageFormatter.serializePayload(payload);
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

    MessageFormatter loadMessageFormatter(final MessageProducerConfig producerConfig) {
        return Optional.ofNullable(formatterMap.get(producerConfig.getFormat()))
                .orElseThrow(() -> new ConfigException.BadValue(producerConfig.getTag() + ".producer.format",
                        "No message formatter plugin found for format: " + producerConfig.getFormat()));
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
