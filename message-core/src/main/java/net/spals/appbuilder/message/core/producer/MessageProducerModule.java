package net.spals.appbuilder.message.core.producer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindModule;
import net.spals.appbuilder.config.ProducerConfig;
import net.spals.appbuilder.message.core.model.Message;

import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindModule
class MessageProducerModule extends AbstractModule {

    private final Map<String, ProducerConfig> producerConfigMap;
    private final Map<String, MessageProducerPlugin> producerPluginMap;

    @Inject
    MessageProducerModule(final Map<String, ProducerConfig> producerConfigMap,
                          final Map<String, MessageProducerPlugin> producerPluginMap) {
        this.producerConfigMap = producerConfigMap;
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

            final MessageProducer delegatingProducer = new DelegatingMessageProducer(producerConfig, producerPlugin);
            final MapBinder mapBinder = MapBinder.newMapBinder(binder(), String.class, MessageProducer.class);
            mapBinder.addBinding(tag).toInstance(delegatingProducer);

            binder().bind(MessageProducer.class).annotatedWith(Names.named(tag)).toInstance(delegatingProducer);
        });
    }

    @VisibleForTesting
    static class DelegatingMessageProducer implements MessageProducer {

        private final ProducerConfig producerConfig;
        private final MessageProducerPlugin pluginDelegate;

        DelegatingMessageProducer(final ProducerConfig producerConfig,
                                  final MessageProducerPlugin pluginDelegate) {
            this.producerConfig = producerConfig;
            this.pluginDelegate = pluginDelegate;
        }

        @Override
        public void sendMessage(final Map<String, Object> payload) {
            pluginDelegate.sendMessage(new Message.Builder()
                    .putAllPayload(payload).setTag(producerConfig.getTag()).build());
        }
    }
}
