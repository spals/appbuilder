package net.spals.appbuilder.app.dropwizard;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import net.spals.appbuilder.app.dropwizard.plugins.PluginsDropwizardWebApp;
import net.spals.appbuilder.filestore.core.FileStore;
import net.spals.appbuilder.filestore.core.FileStorePlugin;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.MapStorePlugin;
import net.spals.appbuilder.message.core.MessageConsumer;
import net.spals.appbuilder.message.core.MessageProducer;
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin;
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.FutureTask;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Functional tests for a plugins {@link DropwizardWebApp}
 *
 * @author tkral
 */
public class PluginsDropwizardWebAppFTest {

    private final DropwizardTestSupport<Configuration> testServerWrapper =
            new DropwizardTestSupport<>(PluginsDropwizardWebApp.class, PluginsDropwizardWebApp.APP_CONFIG_FILE_NAME);
    private DropwizardWebApp webAppDelegate;

    @BeforeTest
    void classSetup() {
        testServerWrapper.before();
        webAppDelegate = ((PluginsDropwizardWebApp)testServerWrapper.getApplication()).getDelegate();
    }

    @AfterTest
    void classTearDown() {
        testServerWrapper.after();
    }

    @Test
    public void testFileStoreInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(FileStore.class), notNullValue());

        final TypeLiteral<Map<String, FileStorePlugin>> fileStorePluginMapKey =
                new TypeLiteral<Map<String, FileStorePlugin>>(){};
        final Map<String, FileStorePlugin> fileStorePluginMap =
                serviceInjector.getInstance(Key.get(fileStorePluginMapKey));
        assertThat(fileStorePluginMap, aMapWithSize(2));
        assertThat(fileStorePluginMap, hasKey("localFS"));
        assertThat(fileStorePluginMap, hasKey("s3"));
    }

    @Test
    public void testMapStoreInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(MapStore.class), notNullValue());

        final TypeLiteral<Map<String, MapStorePlugin>> mapStorePluginMapKey =
                new TypeLiteral<Map<String, MapStorePlugin>>(){};
        final Map<String, MapStorePlugin> mapStorePluginMap =
                serviceInjector.getInstance(Key.get(mapStorePluginMapKey));
        assertThat(mapStorePluginMap, aMapWithSize(3));
        assertThat(mapStorePluginMap, hasKey("cassandra"));
        assertThat(mapStorePluginMap, hasKey("dynamoDB"));
        assertThat(mapStorePluginMap, hasKey("mapDB"));
    }

    @Test
    public void testCassandraMapStoreInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(Cluster.class), notNullValue());
    }

    @Test
    public void testDynamoDBMapStoreInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(AmazonDynamoDB.class), notNullValue());
    }

    @Test
    public void testMessageConsumerInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(MessageConsumer.class), notNullValue());

        final TypeLiteral<Map<String, MessageConsumerPlugin>> messageConsumerPluginMapKey =
                new TypeLiteral<Map<String, MessageConsumerPlugin>>(){};
        final Map<String, MessageConsumerPlugin> messageConsumerPluginMap =
                serviceInjector.getInstance(Key.get(messageConsumerPluginMapKey));
        assertThat(messageConsumerPluginMap, aMapWithSize(3));
        assertThat(messageConsumerPluginMap, hasKey("blockingQueue"));
        assertThat(messageConsumerPluginMap, hasKey("kafka"));
        assertThat(messageConsumerPluginMap, hasKey("kinesis"));
    }

    @Test
    public void testMessageProducerInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(MessageProducer.class), notNullValue());

        final TypeLiteral<Map<String, MessageProducerPlugin>> messageProducerPluginMapKey =
                new TypeLiteral<Map<String, MessageProducerPlugin>>(){};
        final Map<String, MessageProducerPlugin> messageProducerPluginMap =
                serviceInjector.getInstance(Key.get(messageProducerPluginMapKey));
        assertThat(messageProducerPluginMap, aMapWithSize(3));
        assertThat(messageProducerPluginMap, hasKey("blockingQueue"));
        assertThat(messageProducerPluginMap, hasKey("kafka"));
        assertThat(messageProducerPluginMap, hasKey("kinesis"));
    }

    @Test
    public void testModelInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();

        final TypeLiteral<Map<String, ModelSerializer>> modelSerializerMapKey =
                new TypeLiteral<Map<String, ModelSerializer>>(){};
        final Map<String, ModelSerializer> modelSerializerMap =
                serviceInjector.getInstance(Key.get(modelSerializerMapKey));
        assertThat(modelSerializerMap, aMapWithSize(2));
        assertThat(modelSerializerMap, hasKey("pojo"));
        assertThat(modelSerializerMap, hasKey("protobuf"));
    }
}
