package net.spals.appbuilder.message.core.consumer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindModule;
import net.spals.appbuilder.config.ConsumerConfig;

import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindModule
class MessageConsumerModule extends AbstractModule {

    private final Map<String, ConsumerConfig> consumerConfigMap;
    private final Map<String, MessageConsumerPlugin> consumerPluginMap;

    @Inject
    MessageConsumerModule(final Map<String, ConsumerConfig> consumerConfigMap,
                          final Map<String, MessageConsumerPlugin> consumerPluginMap) {
        this.consumerConfigMap = consumerConfigMap;
        this.consumerPluginMap = consumerPluginMap;
    }

    @Override
    protected void configure() {
        consumerConfigMap.entrySet().forEach(consumerConfigEntry -> {
            final String tag = consumerConfigEntry.getKey();
            final ConsumerConfig consumerConfig = consumerConfigEntry.getValue();

            final MessageConsumerPlugin consumerPlugin = Optional.ofNullable(consumerPluginMap.get(consumerConfig.getSource()))
                    .orElseThrow(() -> new ConfigException.BadValue(tag + ".consumer.source",
                            "No message consumer plugin found for source: " + consumerConfig.getSource()));

            // MessageConsumers will never need to be bound individually.
            // Instead, we'll bind them in a set so they can be started at boot time.
            final Multibinder multibinder = Multibinder.newSetBinder(binder(), MessageConsumer.class);
            multibinder.addBinding().toInstance(
                    new DelegatingMessageConsumer(consumerConfig, consumerPlugin));
        });
    }

    @VisibleForTesting
    static class DelegatingMessageConsumer implements MessageConsumer {

        private final ConsumerConfig consumerConfig;
        private final MessageConsumerPlugin pluginDelegate;

        DelegatingMessageConsumer(final ConsumerConfig consumerConfig,
                                  final MessageConsumerPlugin pluginDelegate) {
            this.consumerConfig = consumerConfig;
            this.pluginDelegate = pluginDelegate;
        }

        @Override
        public void start() {
            pluginDelegate.start(consumerConfig);
        }

        @Override
        public void stop() {
            pluginDelegate.stop(consumerConfig);
        }
    }
}
