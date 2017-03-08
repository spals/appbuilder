package net.spals.appbuilder.app.core.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.netflix.governator.ConfigurationModule;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.config.TaggedConfig;
import net.spals.appbuilder.config.message.MessageConsumerConfig;
import net.spals.appbuilder.config.message.MessageProducerConfig;
import net.spals.appbuilder.config.provider.TypesafeConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link BootstrapModule} which auto binds
 * micro-service configuration.
 *
 * @author tkral
 */
public class AutoBindConfigBootstrapModule implements BootstrapModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindConfigBootstrapModule.class);

    private final Config serviceConfig;

    public AutoBindConfigBootstrapModule(final Config serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        // This will parse the configuration and deliver its individual pieces
        // to @Configuration fields.
        bootstrapBinder.bindConfigurationProvider().toInstance(new TypesafeConfigurationProvider(serviceConfig));
        // Also give access to the full blown configuration
        bootstrapBinder.bind(Config.class).annotatedWith(ServiceConfig.class).toInstance(serviceConfig);
        // Enable @Configuration mappings
        bootstrapBinder.install(new ConfigurationModule());

        autoBindConfigs(bootstrapBinder, MessageConsumerConfig.class, parseConfigs("consumer", MessageConsumerConfig.class));
        autoBindConfigs(bootstrapBinder, MessageProducerConfig.class, parseConfigs("producer", MessageProducerConfig.class));
    }

    @VisibleForTesting
    <T extends TaggedConfig> void autoBindConfigs(final BootstrapBinder bootstrapBinder,
                                                  final Class<T> configType,
                                                  final Map<String, T> configMap) {
        final MapBinder mapBinder = MapBinder.newMapBinder(bootstrapBinder, String.class, configType);
        // Bind the config instance in Map<String, T>...
        configMap.entrySet().stream().forEach(configEntry -> {
            mapBinder.addBinding(configEntry.getKey()).toInstance(configEntry.getValue());
            // ...and as a singleton annotated with the tag name
            bootstrapBinder.bind((Class<T>) configEntry.getValue().getClass()).annotatedWith(Names.named(configEntry.getKey()))
                    .toInstance(configEntry.getValue());
        });
    }

    @VisibleForTesting
    <T extends TaggedConfig> Map<String, T> parseConfigs(final String configSubTag, final Class<T> configType) {
        final Set<String> tags = parseTags(configSubTag);
        return tags.stream().map(tag -> {
            // Create a special configuration provider which isolates the tagged configuration object
            // and adds in the implied tag value
            final String configPath = Joiner.on('.').join(tag, configSubTag);
            final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(serviceConfig.withOnlyPath(configPath)
                .withValue(Joiner.on('.').join(tag, configSubTag, "tag"), ConfigValueFactory.fromAnyRef(tag)));

            final ConfigurationKey configKey = new ConfigurationKey(configPath, Collections.emptyList());
            final Supplier<T> configSupplier = configProvider.getObjectSupplier(configKey, null, configType);

            return new AbstractMap.SimpleEntry<>(tag, configSupplier.get());
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @VisibleForTesting
    Set<String> parseTags(final String configSubTag) {
        return serviceConfig.entrySet().stream()
            .map(Map.Entry::getKey)
            .map(configEntryKey -> configEntryKey.split("\\."))
            .filter(parsedConfigEntryKey -> parsedConfigEntryKey.length > 1 && configSubTag.equals(parsedConfigEntryKey[1]))
            .map(parsedConfigEntryKey -> parsedConfigEntryKey[0])
            .collect(Collectors.toSet());
    }
}
