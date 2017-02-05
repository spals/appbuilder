package net.spals.appbuilder.config.provider;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link TypesafeConfigurationProvider}
 *
 * @author tkral
 */
public class TypesafeConfigurationProviderTest {

    @DataProvider
    Object[][] getBooleanProvider() {
        return new Object[][] {
                {ConfigFactory.empty(), "missingKey", false},
                {ConfigFactory.parseMap(ImmutableMap.of("booleanKey", true)), "booleanKey", true},
        };
    }

    @Test(dataProvider = "getBooleanProvider")
    public void testGetBoolean(final Config config, final String rawKey, final Boolean expectedValue) {
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey(rawKey, Collections.emptyList());
        assertThat(configProvider.getBooleanSupplier(configKey, false).get(), is(expectedValue));
    }

    @Test
    public void testGetBooleanInvalid() {
        final Config config = ConfigFactory.parseMap(ImmutableMap.of("invalidKey", "deadbeef"));
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey("invalidKey", Collections.emptyList());
        final Supplier<Boolean> configSupplier = configProvider.getBooleanSupplier(configKey, false);
        verifyException(() -> configSupplier.get(), ConfigException.WrongType.class);
    }

    @DataProvider
    Object[][] getIntegerProvider() {
        return new Object[][] {
                {ConfigFactory.empty(), "missingKey", 0},
                {ConfigFactory.parseMap(ImmutableMap.of("intKey", 1)), "intKey", 1},
        };
    }

    @Test(dataProvider = "getIntegerProvider")
    public void testGetInteger(final Config config, final String rawKey, final Integer expectedValue) {
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey(rawKey, Collections.emptyList());
        assertThat(configProvider.getIntegerSupplier(configKey, 0).get(), is(expectedValue));
    }

    @Test
    public void testGetIntegerInvalid() {
        final Config config = ConfigFactory.parseMap(ImmutableMap.of("invalidKey", "deadbeef"));
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey("invalidKey", Collections.emptyList());
        final Supplier<Integer> configSupplier = configProvider.getIntegerSupplier(configKey, 0);
        verifyException(() -> configSupplier.get(), ConfigException.WrongType.class);
    }

    @DataProvider
    Object[][] getLongProvider() {
        return new Object[][] {
                {ConfigFactory.empty(), "missingKey", 0L},
                {ConfigFactory.parseMap(ImmutableMap.of("longKey", 1L)), "longKey", 1L},
        };
    }

    @Test(dataProvider = "getLongProvider")
    public void testGetLong(final Config config, final String rawKey, final Long expectedValue) {
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey(rawKey, Collections.emptyList());
        assertThat(configProvider.getLongSupplier(configKey, 0L).get(), is(expectedValue));
    }

    @Test
    public void testGetLongInvalid() {
        final Config config = ConfigFactory.parseMap(ImmutableMap.of("invalidKey", "deadbeef"));
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey("invalidKey", Collections.emptyList());
        final Supplier<Long> configSupplier = configProvider.getLongSupplier(configKey, 0L);
        verifyException(() -> configSupplier.get(), ConfigException.WrongType.class);
    }

    @DataProvider
    Object[][] getDoubleProvider() {
        return new Object[][] {
                {ConfigFactory.empty(), "missingKey", 0.0d},
                {ConfigFactory.parseMap(ImmutableMap.of("longKey", 1.0d)), "longKey", 1.0d},
        };
    }

    @Test(dataProvider = "getDoubleProvider")
    public void testGetDouble(final Config config, final String rawKey, final Double expectedValue) {
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey(rawKey, Collections.emptyList());
        assertThat(configProvider.getDoubleSupplier(configKey, 0.0d).get(), is(expectedValue));
    }

    @Test
    public void testGetDoubleInvalid() {
        final Config config = ConfigFactory.parseMap(ImmutableMap.of("invalidKey", "deadbeef"));
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey("invalidKey", Collections.emptyList());
        final Supplier<Double> configSupplier = configProvider.getDoubleSupplier(configKey, 0.0d);
        verifyException(() -> configSupplier.get(), ConfigException.WrongType.class);
    }

    @DataProvider
    Object[][] getStringProvider() {
        return new Object[][] {
                {ConfigFactory.empty(), "missingKey", ""},
                {ConfigFactory.parseMap(ImmutableMap.of("stringKey", "str")), "stringKey", "str"},
        };
    }

    @Test(dataProvider = "getStringProvider")
    public void testGetString(final Config config, final String rawKey, final String expectedValue) {
        final ConfigurationProvider configProvider = new TypesafeConfigurationProvider(config);

        final ConfigurationKey configKey = new ConfigurationKey(rawKey, Collections.emptyList());
        assertThat(configProvider.getStringSupplier(configKey, "").get(), is(expectedValue));
    }
}
