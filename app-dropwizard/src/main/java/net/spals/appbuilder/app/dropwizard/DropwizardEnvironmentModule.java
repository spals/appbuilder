package net.spals.appbuilder.app.dropwizard;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import io.dropwizard.setup.Environment;

import javax.validation.Validator;

/**
 * A {@link Module} for binding
 * common Dropwizard {@link Environment} state.
 *
 * @author tkral
 */
class DropwizardEnvironmentModule extends AbstractModule {

    private final Environment env;

    DropwizardEnvironmentModule(final Environment env) {
        this.env = env;
    }

    @Override
    public void configure() {
        binder().bind(Environment.class).toInstance(env);
        binder().bind(Validator.class).toInstance(env.getValidator());
    }
}
