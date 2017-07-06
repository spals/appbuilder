package net.spals.appbuilder.app.core.jaxrs;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import com.typesafe.config.Config;
import net.spals.appbuilder.app.core.generic.MinimalGenericWorkerAppFTest;
import net.spals.appbuilder.app.core.sample.SampleCoreBootstrapModule;
import net.spals.appbuilder.app.core.sample.SampleCoreCustomService;
import net.spals.appbuilder.app.core.sample.SampleCoreGuiceModule;
import net.spals.appbuilder.app.core.sample.web.*;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.executor.core.ExecutorServiceFactory;
import net.spals.appbuilder.filestore.core.FileStore;
import net.spals.appbuilder.filestore.core.FileStorePlugin;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.MapStorePlugin;
import net.spals.appbuilder.message.core.MessageConsumer;
import net.spals.appbuilder.message.core.MessageConsumerCallback;
import net.spals.appbuilder.message.core.MessageProducer;
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin;
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.ws.rs.core.Configurable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Functional tests for a sample {@link JaxRsWebApp}
 *
 * @author tkral
 */
public class SampleJaxRsWebAppFTest {
    private final Logger LOGGER = LoggerFactory.getLogger(MinimalGenericWorkerAppFTest.class);

    private final Configurable<?> configurable = mock(Configurable.class);
    private final Map<String, Filter> registeredFilters = new HashMap<>();
    private final BiFunction<String, Filter, FilterRegistration.Dynamic> filterRegistration = new BiFunction<String, Filter, FilterRegistration.Dynamic>() {
        @Override
        public FilterRegistration.Dynamic apply(final String name, final Filter filter) {
            registeredFilters.put(name, filter);
            return mock(FilterRegistration.Dynamic.class);
        }
    };

    private final JaxRsWebApp sampleApp = new JaxRsWebApp.Builder("sample", LOGGER)
            .setConfigurable(configurable)
            .setFilterRegistration(filterRegistration)
            .setServiceConfigFromClasspath("config/sample-jaxrs-service.conf")
            .setServiceScan(new ServiceScan.Builder()
                    .addServicePackages("net.spals.appbuilder.app.core.sample")
                    .addDefaultServices(ExecutorServiceFactory.class)
                    .addDefaultServices(FileStore.class)
                    .addDefaultServices(MapStore.class)
                    .addDefaultServices(MessageConsumer.class, MessageProducer.class)
                    .addDefaultServices(ModelSerializer.class)
                    .build())
            .addBootstrapModule(new SampleCoreBootstrapModule())
            .addModule(new SampleCoreGuiceModule())
            .enableRequestScoping()
            .build();

    @DataProvider
    Object[][] serviceConfigProvider() {
        return new Object[][] {
                {"fileStore.system", "localFS"},
                {"mapStore.system", "mapDB"},
        };
    }

    @Test(dataProvider = "serviceConfigProvider")
    public void testServiceConfig(final String configKey, final Object expectedConfigValue) {
        final Config serviceConfig = sampleApp.getServiceConfig();
        assertThat(serviceConfig.getAnyRef(configKey), is(expectedConfigValue));
    }

    @DataProvider
    Object[][] customModuleInjectionProvider() {
        return new Object[][] {
                {"AutoBoundModule", "sample:net.spals.appbuilder.app.core.sample.SampleCoreAutoBoundModule"},
                {"BootstrapModule", "net.spals.appbuilder.app.core.sample.SampleCoreBootstrapModule"},
                {"GuiceModule", "net.spals.appbuilder.app.core.sample.SampleCoreGuiceModule"},
        };
    }

    @Test(dataProvider = "customModuleInjectionProvider")
    public void testCustomModuleInjection(final String keyName, final String expectedBindValue) {
        final Injector serviceInjector = sampleApp.getServiceInjector();
        assertThat(serviceInjector.getInstance(Key.get(String.class, Names.named(keyName))),
                is(expectedBindValue));
    }

    @Test
    public void testCustomServiceInjection() {
        final Injector serviceInjector = sampleApp.getServiceInjector();
        assertThat(serviceInjector.getInstance(SampleCoreCustomService.class), notNullValue());
    }

    @Test
    public void testExecutorInjection() {
        final Injector serviceInjector = sampleApp.getServiceInjector();
        assertThat(serviceInjector.getInstance(ExecutorServiceFactory.class), notNullValue());
    }

    @Test
    public void testFileStoreInjection() {
        final Injector serviceInjector = sampleApp.getServiceInjector();
        assertThat(serviceInjector.getInstance(FileStore.class), notNullValue());

        final TypeLiteral<Map<String, FileStorePlugin>> fileStorePluginMapKey =
                new TypeLiteral<Map<String, FileStorePlugin>>(){};
        final Map<String, FileStorePlugin> fileStorePluginMap =
                serviceInjector.getInstance(Key.get(fileStorePluginMapKey));
        assertThat(fileStorePluginMap, aMapWithSize(1));
        assertThat(fileStorePluginMap, hasKey("localFS"));
    }

    @Test
    public void testMapStoreInjection() {
        final Injector serviceInjector = sampleApp.getServiceInjector();
        assertThat(serviceInjector.getInstance(MapStore.class), notNullValue());

        final TypeLiteral<Map<String, MapStorePlugin>> mapStorePluginMapKey =
                new TypeLiteral<Map<String, MapStorePlugin>>(){};
        final Map<String, MapStorePlugin> mapStorePluginMap =
                serviceInjector.getInstance(Key.get(mapStorePluginMapKey));
        assertThat(mapStorePluginMap, aMapWithSize(1));
        assertThat(mapStorePluginMap, hasKey("mapDB"));
    }

    @Test
    public void testMessageConsumerInjection() {
        final Injector serviceInjector = sampleApp.getServiceInjector();
        assertThat(serviceInjector.getInstance(MessageConsumer.class), notNullValue());

        final TypeLiteral<Map<String, MessageConsumerPlugin>> messageConsumerPluginMapKey =
                new TypeLiteral<Map<String, MessageConsumerPlugin>>(){};
        final Map<String, MessageConsumerPlugin> messageConsumerPluginMap =
                serviceInjector.getInstance(Key.get(messageConsumerPluginMapKey));
        assertThat(messageConsumerPluginMap, aMapWithSize(1));
        assertThat(messageConsumerPluginMap, hasKey("blockingQueue"));
    }

    @Test
    public void testMessageConsumerCallbackInjection() {
        final Injector serviceInjector = sampleApp.getServiceInjector();

        final TypeLiteral<Set<MessageConsumerCallback<?>>> messageCallbackSetKey =
                new TypeLiteral<Set<MessageConsumerCallback<?>>>(){};
        final Set<MessageConsumerCallback<?>> messageCallbackSet =
                serviceInjector.getInstance(Key.get(messageCallbackSetKey));
        assertThat(messageCallbackSet, notNullValue());
    }

    @Test
    public void testMessageProducerInjection() {
        final Injector serviceInjector = sampleApp.getServiceInjector();
        assertThat(serviceInjector.getInstance(MessageProducer.class), notNullValue());

        final TypeLiteral<Map<String, MessageProducerPlugin>> messageProducerPluginMapKey =
                new TypeLiteral<Map<String, MessageProducerPlugin>>(){};
        final Map<String, MessageProducerPlugin> messageProducerPluginMap =
                serviceInjector.getInstance(Key.get(messageProducerPluginMapKey));
        assertThat(messageProducerPluginMap, aMapWithSize(1));
        assertThat(messageProducerPluginMap, hasKey("blockingQueue"));
    }

    @Test
    public void testModelInjection() {
        final Injector serviceInjector = sampleApp.getServiceInjector();

        final TypeLiteral<Map<String, ModelSerializer>> modelSerializerMapKey =
                new TypeLiteral<Map<String, ModelSerializer>>(){};
        final Map<String, ModelSerializer> modelSerializerMap =
                serviceInjector.getInstance(Key.get(modelSerializerMapKey));
        assertThat(modelSerializerMap, aMapWithSize(1));
        assertThat(modelSerializerMap, hasKey("pojo"));
    }

    @Test
    public void testRequestScopeEnabled() {
        assertThat(registeredFilters, hasEntry(is("com.google.inject.servlet.GuiceFilter"), instanceOf(GuiceFilter.class)));
    }

    @DataProvider
    Object[][] webInjectionProvider() {
        return new Object[][] {
                {SampleCoreDynamicFeature.class},
                {SampleCoreExceptionMapper.class},
                {SampleCoreProvider.class},
                {SampleCoreRequestFilter.class},
                {SampleCoreResource.class},
                {SampleCoreResponseFilter.class},
        };
    }

    @Test(dataProvider = "webInjectionProvider")
    public void testWebInjection(final Class<?> webClazz) {
        verify(configurable).register(isA(webClazz));
    }
}
