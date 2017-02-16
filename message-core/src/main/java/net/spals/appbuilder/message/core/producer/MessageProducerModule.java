package net.spals.appbuilder.message.core.producer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindModule;
import net.spals.appbuilder.config.ProducerConfig;
import net.spals.appbuilder.message.core.formatter.MessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindModule
class MessageProducerModule extends AbstractModule {

    private final Map<String, ProducerConfig> producerConfigMap;

    private final Map<String, MessageFormatter> formatterMap;
    private final Map<String, MessageProducerPlugin> producerPluginMap;

    @Inject
    MessageProducerModule(final Map<String, ProducerConfig> producerConfigMap,
                          final Map<String, MessageFormatter> formatterMap,
                          final Map<String, MessageProducerPlugin> producerPluginMap) {
        this.producerConfigMap = producerConfigMap;

        this.formatterMap = formatterMap;
        this.producerPluginMap = producerPluginMap;
    }

    @Override
    protected void configure() {
        producerConfigMap.entrySet().forEach(producerConfigEntry -> {
            final String tag = producerConfigEntry.getKey();
            final ProducerConfig producerConfig = producerConfigEntry.getValue();

            final MessageProducerPlugin producerPlugin = Optional.ofNullable(producerPluginMap.get(producerConfig.getDestination()))
                    .orElseThrow(() -> new ConfigException.BadValue(tag + ".producer.destination",
                            "No message producer plugin found for destination: " + producerConfig.getDestination()));

            final MessageFormatter formatter = Optional.ofNullable(formatterMap.get(producerConfig.getFormat()))
                    .orElseThrow(() -> new ConfigException.BadValue(tag + ".producer.format",
                            "No message formatter plugin found for format: " + producerConfig.getFormat()));

            final MessageProducer delegatingProducer =
                    new DelegatingMessageProducer(producerConfig, formatter, producerPlugin);
            final MapBinder mapBinder = MapBinder.newMapBinder(binder(), String.class, MessageProducer.class);
            mapBinder.addBinding(tag).toInstance(delegatingProducer);

            binder().bind(MessageProducer.class).annotatedWith(Names.named(tag)).toInstance(delegatingProducer);
        });
    }

    @VisibleForTesting
    static class DelegatingMessageProducer implements MessageProducer {
        private final Logger logger;
        private final ProducerConfig producerConfig;

        private final MessageFormatter formatter;
        private final MessageProducerPlugin pluginDelegate;

        DelegatingMessageProducer(final ProducerConfig producerConfig,
                                  final MessageFormatter formatter,
                                  final MessageProducerPlugin pluginDelegate) {
            this.logger = LoggerFactory.getLogger(DelegatingMessageProducer.class.getName() + "[" + producerConfig.getTag() + "]");
            this.producerConfig = producerConfig;

            this.formatter = formatter;
            this.pluginDelegate = pluginDelegate;
        }

        @Override
        public void sendMessage(final Map<String, Object> payload) {
            final byte[] serializedPayload;
            try {
                serializedPayload = formatter.serializePayload(payload);
            } catch (IOException e) {
                logger.error("Error while serializing message payload with " + producerConfig.getFormat(), e);
                return;
            }

            try {
                pluginDelegate.sendMessage(producerConfig, serializedPayload);
            } catch (IOException e) {
                logger.error("Error while sending message to " + producerConfig.getDestination(), e);
                return;
            }
        }
    }
}
