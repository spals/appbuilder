package net.spals.appbuilder.app.dropwizard;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.datastax.driver.core.Cluster;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import io.opentracing.NoopTracer;
import io.opentracing.Tracer;
import net.spals.appbuilder.app.examples.dropwizard.plugins.PluginsDropwizardWebApp;
import net.spals.appbuilder.filestore.core.FileStore;
import net.spals.appbuilder.filestore.core.FileStorePlugin;
import net.spals.appbuilder.keystore.core.KeyStore;
import net.spals.appbuilder.keystore.core.KeyStorePlugin;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.MapStoreIndex;
import net.spals.appbuilder.mapstore.core.MapStoreIndexPlugin;
import net.spals.appbuilder.mapstore.core.MapStorePlugin;
import net.spals.appbuilder.message.core.MessageConsumer;
import net.spals.appbuilder.message.core.MessageProducer;
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin;
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin;
import net.spals.appbuilder.model.core.ModelSerializer;
import net.spals.appbuilder.monitor.core.TracerPlugin;
import net.spals.appbuilder.monitor.core.TracerTag;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

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

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
        webAppDelegate = ((PluginsDropwizardWebApp)testServerWrapper.getApplication()).getDelegate();
    }

    @AfterClass
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
    public void testKeyStoreInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(KeyStore.class), notNullValue());

        final TypeLiteral<Map<String, KeyStorePlugin>> keyStorePluginMapKey =
            new TypeLiteral<Map<String, KeyStorePlugin>>(){};
        final Map<String, KeyStorePlugin> keyStorePluginMap =
            serviceInjector.getInstance(Key.get(keyStorePluginMapKey));
        assertThat(keyStorePluginMap, aMapWithSize(1));
        assertThat(keyStorePluginMap, hasKey("password"));
    }

    @Test
    public void testMapStoreInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(MapStore.class), notNullValue());

        final TypeLiteral<Map<String, MapStorePlugin>> mapStorePluginMapKey =
            new TypeLiteral<Map<String, MapStorePlugin>>(){};
        final Map<String, MapStorePlugin> mapStorePluginMap =
            serviceInjector.getInstance(Key.get(mapStorePluginMapKey));
        assertThat(mapStorePluginMap, aMapWithSize(4));
        assertThat(mapStorePluginMap, hasKey("cassandra"));
        assertThat(mapStorePluginMap, hasKey("dynamoDB"));
        assertThat(mapStorePluginMap, hasKey("mapDB"));
        assertThat(mapStorePluginMap, hasKey("mongoDB"));
    }

    @Test
    public void testMapStoreIndexInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(MapStoreIndex.class), notNullValue());

        final TypeLiteral<Map<String, MapStoreIndexPlugin>> mapStoreIndexPluginMapKey =
            new TypeLiteral<Map<String, MapStoreIndexPlugin>>(){};
        final Map<String, MapStoreIndexPlugin> mapStoreIndexPluginMap =
            serviceInjector.getInstance(Key.get(mapStoreIndexPluginMapKey));
        assertThat(mapStoreIndexPluginMap, aMapWithSize(3));
        assertThat(mapStoreIndexPluginMap, hasKey("dynamoDB"));
        assertThat(mapStoreIndexPluginMap, hasKey("mapDB"));
        assertThat(mapStoreIndexPluginMap, hasKey("mongoDB"));
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
        assertThat(messageConsumerPluginMap, aMapWithSize(2));
        assertThat(messageConsumerPluginMap, hasKey("blockingQueue"));
        assertThat(messageConsumerPluginMap, hasKey("kafka"));
    }

    @Test
    public void testMessageProducerInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(MessageProducer.class), notNullValue());

        final TypeLiteral<Map<String, MessageProducerPlugin>> messageProducerPluginMapKey =
            new TypeLiteral<Map<String, MessageProducerPlugin>>(){};
        final Map<String, MessageProducerPlugin> messageProducerPluginMap =
            serviceInjector.getInstance(Key.get(messageProducerPluginMapKey));
        assertThat(messageProducerPluginMap, aMapWithSize(2));
        assertThat(messageProducerPluginMap, hasKey("blockingQueue"));
        assertThat(messageProducerPluginMap, hasKey("kafka"));
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

    @Test
    public void testTracerInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();
        assertThat(serviceInjector.getInstance(Tracer.class), instanceOf(NoopTracer.class));

        final TypeLiteral<Map<String, TracerPlugin>> tracerPluginMapKey =
            new TypeLiteral<Map<String, TracerPlugin>>(){};
        final Map<String, TracerPlugin> tracerPluginMap =
            serviceInjector.getInstance(Key.get(tracerPluginMapKey));
        assertThat(tracerPluginMap, aMapWithSize(2));
        assertThat(tracerPluginMap, hasKey("lightstep"));
        assertThat(tracerPluginMap, hasKey("noop"));
    }

    @Test
    public void testTracerTagInjection() {
        final Injector serviceInjector = webAppDelegate.getServiceInjector();

        final TypeLiteral<Map<String, TracerTag>> tracerTagMapKey =
            new TypeLiteral<Map<String, TracerTag>>(){};
        final Map<String, TracerTag> tracerTagMap =
            serviceInjector.getInstance(Key.get(tracerTagMapKey));
        assertThat(tracerTagMap, aMapWithSize(2));
        assertThat(tracerTagMap, hasEntry(is("key1"),
            is(new TracerTag.Builder().setTag("key1").setValue("value").build())));
        assertThat(tracerTagMap, hasEntry(is("key2"),
            is(new TracerTag.Builder().setTag("key2").setValue(2).build())));
    }
}
