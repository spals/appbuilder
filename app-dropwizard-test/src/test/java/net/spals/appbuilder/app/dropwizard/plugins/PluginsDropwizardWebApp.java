package net.spals.appbuilder.app.dropwizard.plugins;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import net.spals.appbuilder.app.dropwizard.DropwizardWebApp;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.filestore.core.FileStore;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.message.core.MessageConsumer;
import net.spals.appbuilder.message.core.MessageProducer;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DropwizardWebApp} which uses all various service plugins.
 *
 * @author tkral
 */
public class PluginsDropwizardWebApp extends Application<Configuration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginsDropwizardWebApp.class);

    @VisibleForTesting
    public static final String APP_CONFIG_FILE_NAME = "config/plugins-dropwizard-app.yml";
    private static final String SERVICE_CONFIG_FILE_NAME = "config/plugins-dropwizard-service.conf";

    public static void main(final String[] args) throws Throwable {
        new PluginsDropwizardWebApp().run("server", APP_CONFIG_FILE_NAME);
    }

    private DropwizardWebApp.Builder webAppDelegateBuilder;
    private DropwizardWebApp webAppDelegate;

    @VisibleForTesting
    public DropwizardWebApp getDelegate() {
        return webAppDelegate;
    }

    @Override
    public void initialize(final Bootstrap<Configuration> bootstrap) {
        this.webAppDelegateBuilder = new DropwizardWebApp.Builder(bootstrap, LOGGER)
            .setServiceConfigFromClasspath(SERVICE_CONFIG_FILE_NAME)
            .setServiceScan(new ServiceScan.Builder()
                .addServicePackages("net.spals.appbuilder.app.dropwizard.plugins")
                .addServicePlugins("net.spals.appbuilder.filestore.s3", FileStore.class)
                .addServicePlugins("net.spals.appbuilder.mapstore.cassandra", MapStore.class)
                .addServicePlugins("net.spals.appbuilder.mapstore.dynamodb", MapStore.class)
                .addServicePlugins("net.spals.appbuilder.message.kafka", MessageConsumer.class, MessageProducer.class)
                .addServicePlugins("net.spals.appbuilder.message.kinesis", MessageConsumer.class, MessageProducer.class)
                .addServicePlugins("net.spals.appbuilder.model.protobuf", ModelSerializer.class)
                .addServicePlugins("net.spals.appbuilder.monitor.lightstep", Tracer.class)
                .build());
    }

    @Override
    public void run(Configuration configuration, Environment env) throws Exception {
        this.webAppDelegate = webAppDelegateBuilder.setEnvironment(env).build();
    }
}
