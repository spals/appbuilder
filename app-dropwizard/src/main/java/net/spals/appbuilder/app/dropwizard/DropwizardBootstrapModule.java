package net.spals.appbuilder.app.dropwizard;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import io.dropwizard.setup.Bootstrap;

/**
 * A {@link Module} for binding
 * common Dropwizard {@link Bootstrap} state.
 *
 * @author tkral
 */
class DropwizardBootstrapModule extends AbstractModule {

    private final Bootstrap<?> bootstrap;

    DropwizardBootstrapModule(final Bootstrap<?> bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void configure() {
        binder().bind(HealthCheckRegistry.class).toInstance(bootstrap.getHealthCheckRegistry());
        binder().bind(MetricRegistry.class).toInstance(bootstrap.getMetricRegistry());
    }
}
