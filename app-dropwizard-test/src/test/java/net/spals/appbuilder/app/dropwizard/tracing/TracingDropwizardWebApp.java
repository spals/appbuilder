package net.spals.appbuilder.app.dropwizard.tracing;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
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
 * A minimally viable {@link DropwizardWebApp}
 *
 * @author tkral
 */
public class TracingDropwizardWebApp extends Application<Configuration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TracingDropwizardWebApp.class);

    public static void main(final String[] args) throws Throwable {
        new TracingDropwizardWebApp().run("server");
    }

    private DropwizardWebApp.Builder webAppDelegateBuilder;
    private final MockTracer mockTracer = new MockTracer();

    @VisibleForTesting
    public MockTracer getMockTracer() {
        return mockTracer;
    }

    @Override
    public void initialize(final Bootstrap<Configuration> bootstrap) {
        this.webAppDelegateBuilder = new DropwizardWebApp.Builder(bootstrap, LOGGER)
                .setServiceScan(new ServiceScan.Builder()
                        .addServicePackages("net.spals.appbuilder.app.dropwizard.tracing")
                        .build())
                .addModule(binder -> binder.bind(Tracer.class).toInstance(mockTracer))
                // Allow the mock tracer to override the real tracer instance
                .enableBindingOverrides();
    }

    @Override
    public void run(Configuration configuration, Environment env) throws Exception {
        webAppDelegateBuilder.setEnvironment(env).build();
    }
}
