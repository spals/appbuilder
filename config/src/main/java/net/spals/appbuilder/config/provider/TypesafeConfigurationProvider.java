package net.spals.appbuilder.config.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.annotations.VisibleForTesting;
import com.netflix.governator.configuration.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.keystore.core.KeyStore;
import net.spals.appbuilder.keystore.core.KeyStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A {@link ConfigurationProvider} which is backed
 * by a Typesafe {@link Config} object.
 *
 * @author tkral
 */
public class TypesafeConfigurationProvider extends DefaultConfigurationProvider {
    @VisibleForTesting
    static final Pattern ENCRYPTED_PATTERN = Pattern.compile("^ENC\\((.*)\\)");
    private static final Logger LOGGER = LoggerFactory.getLogger(TypesafeConfigurationProvider.class);

    private final Config config;

    private final AtomicReference<KeyStore> keyStoreRef = new AtomicReference<>();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module());

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
                    final String stringProperty = config.getString(key.getRawKey());
                    // Check to see if the string property is encrypted
                    final Matcher encryptedMatcher = ENCRYPTED_PATTERN.matcher(stringProperty);
                    if (encryptedMatcher.matches()) {
                        final String encryptedStringProperty = encryptedMatcher.group(1);
                        try {
                            // KeyStores may be expensive to set up, so ensure that we only load it once
                            // (or, at least, try our best to ensure)
                            final KeyStore keyStore = Optional.ofNullable(keyStoreRef.get())
                                .orElseGet(() -> {
                                    keyStoreRef.compareAndSet(null, KeyStoreProvider.createKeyStore(config));
                                    return keyStoreRef.get();
                                });
                            return keyStore.decrypt(encryptedStringProperty);
                        } catch (ConfigException e) {
                            throw new ConfigException.Generic(
                                "The configuration uses an encrypted value at " + key.getRawKey() +
                                    ", but it is not set up to handle encrypted values", e);
                        }
                    }

                    return stringProperty;
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
