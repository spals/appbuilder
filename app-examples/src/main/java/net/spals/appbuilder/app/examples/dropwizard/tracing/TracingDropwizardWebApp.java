package net.spals.appbuilder.app.examples.dropwizard.tracing;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import net.spals.appbuilder.app.dropwizard.DropwizardWebApp;
import net.spals.appbuilder.config.service.ServiceScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A minimally viable {@link DropwizardWebApp}
 * with API request tracing enabled
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
                        .addServicePackages("net.spals.appbuilder.app.examples.dropwizard.tracing")
                        .build())
                // We'll explicitly bind a mock tracer for testing purposes. But normally
                // this only requires a dependency on a monitor plugin (e.g. monitor-lightstep).
                .addModule(binder -> binder.bind(Tracer.class).toInstance(mockTracer))
                // Allow the mock tracer to override the real tracer instance
                .enableBindingOverrides();
    }

    @Override
    public void run(Configuration configuration, Environment env) throws Exception {
        webAppDelegateBuilder.setEnvironment(env).build();
    }
}
