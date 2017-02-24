package net.spals.appbuilder.app.bootstrap;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import io.dropwizard.setup.Environment;

/**
 * @author tkral
 */
public class DropwizardBootstrapModule implements BootstrapModule {

    private final Environment dropwizardEnv;

    public DropwizardBootstrapModule(final Environment dropwizardEnv) {
        this.dropwizardEnv = dropwizardEnv;
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        // Bind standard DropWizard state
        bootstrapBinder.bind(Environment.class).toInstance(dropwizardEnv);
        bootstrapBinder.bind(HealthCheckRegistry.class).toInstance(dropwizardEnv.healthChecks());
        bootstrapBinder.bind(MetricRegistry.class).toInstance(dropwizardEnv.metrics());
    }
}
