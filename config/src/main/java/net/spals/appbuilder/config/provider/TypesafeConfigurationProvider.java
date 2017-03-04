package net.spals.appbuilder.config.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.governator.configuration.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link ConfigurationProvider} which is backed
 * by a Typesafe {@link Config} object.
 *
 * @author tkral
 */
public class TypesafeConfigurationProvider extends DefaultConfigurationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TypesafeConfigurationProvider.class);

    private final Config config;
    private final ObjectMapper mapper = new ObjectMapper();

    public TypesafeConfigurationProvider(final Config config) {
        this.config = config;
    }

    @Override
    public boolean has(final ConfigurationKey key) {
        return config.hasPath(key.getRawKey());
    }

    @Override
    public Property<Boolean> getBooleanProperty(final ConfigurationKey key, final Boolean defaultValue) {
        return new Property<Boolean>() {
            @Override
            public Boolean get() {
                try {
                    return config.getBoolean(key.getRawKey());
                } catch (ConfigException.Missing e) {
                    return defaultValue;
                }
            }
        };
    }

    @Override
    public Property<Integer> getIntegerProperty(final ConfigurationKey key, final Integer defaultValue) {
        return new Property<Integer>() {
            @Override
            public Integer get() {
                try {
                    return config.getInt(key.getRawKey());
                } catch (ConfigException.Missing e) {
                    return defaultValue;
                }
            }
        };
    }

    @Override
    public Property<Long> getLongProperty(final ConfigurationKey key, final Long defaultValue) {
        return new Property<Long>() {
            @Override
            public Long get() {
                try {
                    return config.getLong(key.getRawKey());
                } catch (ConfigException.Missing e) {
                    return defaultValue;
                }
            }
        };
    }

    @Override
    public Property<Double> getDoubleProperty(final ConfigurationKey key, final Double defaultValue) {
        return new Property<Double>() {
            @Override
            public Double get() {
                try {
                    return config.getDouble(key.getRawKey());
                } catch (ConfigException.Missing e) {
                    return defaultValue;
                }
            }
        };
    }

    @Override
    public Property<String> getStringProperty(final ConfigurationKey key, final String defaultValue) {
        return new Property<String>() {
            @Override
            public String get() {
                try {
                    return config.getString(key.getRawKey());
                } catch (ConfigException.Missing e) {
                    return defaultValue;
                }
            }
        };
    }

    @Override
    public Property<Date> getDateProperty(final ConfigurationKey key, final Date defaultValue) {
        return new DateWithDefaultProperty(getStringProperty(key, null), defaultValue);
    }

    @Override
    public <T> Property<T> getObjectProperty(final ConfigurationKey key,  final T defaultValue, final Class<T> objectType) {
        return new Property<T>() {
            @Override
            public T get() {
                final Config configValue = config.getConfig(key.getRawKey());
                final Map<String, Object> configValueMap = configValue.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().unwrapped()));

                try {
                    final byte[] serialized = mapper.writeValueAsBytes(configValueMap);
                    return mapper.readValue(serialized, objectType);
                } catch (IOException e) {
                    LOGGER.warn("Could not deserialize configuration with key " + key.getRawKey()
                            + " to type " + objectType, e);
                    return defaultValue;
                }
            }
        };
    }
}
