package net.spals.appbuilder.app.dropwizard;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import io.dropwizard.setup.Bootstrap;

/**
 * A {@link BootstrapModule} for binding
 * common Dropwizard {@link Bootstrap} state.
 *
 * @author tkral
 */
class DropwizardBootstrapBootstrapModule implements BootstrapModule {

    private final Bootstrap<?> bootstrap;

    DropwizardBootstrapBootstrapModule(final Bootstrap<?> bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        bootstrapBinder.bind(HealthCheckRegistry.class).toInstance(bootstrap.getHealthCheckRegistry());
        bootstrapBinder.bind(MetricRegistry.class).toInstance(bootstrap.getMetricRegistry());
    }
}
