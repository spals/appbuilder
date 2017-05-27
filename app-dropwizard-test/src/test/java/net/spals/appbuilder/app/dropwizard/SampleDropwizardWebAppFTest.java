package net.spals.appbuilder.app.dropwizard;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import net.spals.appbuilder.app.dropwizard.sample.SampleCustomService;
import net.spals.appbuilder.app.dropwizard.sample.SampleDropwizardWebApp;
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
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Functional tests for a sample {@link DropwizardWebApp}
 *
 * @author tkral
 */
public class SampleDropwizardWebAppFTest {

    private final DropwizardTestSupport<Configuration> testServerWrapper =
            new DropwizardTestSupport<>(SampleDropwizardWebApp.class, SampleDropwizardWebApp.APP_CONFIG_FILE_NAME);
    private DropwizardWebApp webAppDelegate;

    @BeforeTest
    void classSetup() {
        testServerWrapper.before();
        webAppDelegate = ((SampleDropwizardWebApp)testServerWrapper.getApplication()).getDelegate();
    }

    @AfterTest
    void classTearDown() {
        testServerWrapper.after();
    }

    @DataProvider
    Object[][] serviceConfigProvider() {
        return new Object[][] {
                {"fileStore.system", "localFS"},
                {"mapStore.system", "mapDB"},
        };
    }

    @Test(dataProvider = "serviceConfigProvider")
    public void testServiceConfig(final String configKey, final Object expectedConfigValue) {
        final Config serviceConfig = webAppDelegate.getServiceConfig();
        assertThat(serviceConfig.getAnyRef(configKey), is(expectedConfigValue));
    }

    @DataProvider
    Object[][] customModuleInjectionProvider() {
        return new Object[][] {
                {"AutoBoundModule", "SampleDropwizardWebApp:net.spals.appbuilder.app.dropwizard.sample.SampleAutoBoundModule"},
                {"BootstrapModule", "net.spals.appbuilder.app.dropwizard.sample.SampleBootstrapModule"},
                {"GuiceModule", "net.spals.appbuilder.app.dropwizard.sample.SampleGuiceModule"},
        };
    }

    @Test(dataProvider = "customModuleInjectionProvider")
    public void testCustomModuleInjection(final String keyName, final String expectedBindValue) {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(Key.get(String.class, Names.named(keyName))),
                is(expectedBindValue));
    }

    @Test
    public void testCustomServiceInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(SampleCustomService.class), notNullValue());
    }

    @Test
    public void testExecutorInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(ExecutorServiceFactory.class), notNullValue());
    }

    @Test
    public void testFileStoreInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
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
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
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
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
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
        final Injector serviceInjector = webAppDelegate.getServiceInjector();

        final TypeLiteral<Set<MessageConsumerCallback<?>>> messageCallbackSetKey =
                new TypeLiteral<Set<MessageConsumerCallback<?>>>(){};
        final Set<MessageConsumerCallback<?>> messageCallbackSet =
                serviceInjector.getInstance(Key.get(messageCallbackSetKey));
        assertThat(messageCallbackSet, notNullValue());
    }

    @Test
    public void testMessageProducerInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
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
        final Injector serviceInjector = webAppDelegate.getServiceInjector();

        final TypeLiteral<Map<String, ModelSerializer>> modelSerializerMapKey =
                new TypeLiteral<Map<String, ModelSerializer>>(){};
        final Map<String, ModelSerializer> modelSerializerMap =
                serviceInjector.getInstance(Key.get(modelSerializerMapKey));
        assertThat(modelSerializerMap, aMapWithSize(1));
        assertThat(modelSerializerMap, hasKey("pojo"));
    }
}
