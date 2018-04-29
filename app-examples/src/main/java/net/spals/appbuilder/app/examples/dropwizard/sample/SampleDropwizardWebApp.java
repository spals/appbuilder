package net.spals.appbuilder.app.examples.dropwizard.sample;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.spals.appbuilder.app.dropwizard.DropwizardWebApp;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.filestore.core.FileStore;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.keystore.core.KeyStore;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.message.core.MessageConsumer;
import net.spals.appbuilder.message.core.MessageProducer;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A full sample {@link DropwizardWebApp} which uses all default services
 * and bindings.
 *
 * @author tkral
 */
public class SampleDropwizardWebApp extends Application<Configuration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleDropwizardWebApp.class);

    @VisibleForTesting
    public static final String APP_CONFIG_FILE_NAME = "config/sample-dropwizard-app.yml";
    private static final String SERVICE_CONFIG_FILE_NAME = "config/sample-dropwizard-service.conf";

    public static void main(final String[] args) throws Throwable {
        new SampleDropwizardWebApp().run("server", APP_CONFIG_FILE_NAME);
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
            .enableServiceGraph(ServiceGraphFormat.ASCII)
            .setServiceConfigFromClasspath(SERVICE_CONFIG_FILE_NAME)
            .setServiceScan(new ServiceScan.Builder()
                .addServicePackages("net.spals.appbuilder.app.examples.dropwizard.sample")
                .addDefaultServices(FileStore.class)
                .addDefaultServices(KeyStore.class)
                .addDefaultServices(MapStore.class)
                .addDefaultServices(MessageConsumer.class, MessageProducer.class)
                .addDefaultServices(ModelSerializer.class)
                .build())
            .addBootstrapModule(new SampleDropwizardBootstrapModule())
            .addModule(new SampleDropwizardGuiceModule());
    }

    @Override
    public void run(final Configuration configuration, final Environment env) throws Exception {
        this.webAppDelegate = webAppDelegateBuilder.setEnvironment(env).build();
    }
}
