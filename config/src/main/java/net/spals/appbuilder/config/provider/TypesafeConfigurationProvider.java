package net.spals.appbuilder.config.provider;

import com.netflix.governator.configuration.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import java.util.Date;

/**
 * A {@link ConfigurationProvider} which is backed
 * by a Typesafe {@link Config} object.
 *
 * @author tkral
 */
public class TypesafeConfigurationProvider extends AbstractObjectConfigurationProvider {

    private final Config config;

    public TypesafeConfigurationProvider(final Config config) {
        this.config = config;
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
}
