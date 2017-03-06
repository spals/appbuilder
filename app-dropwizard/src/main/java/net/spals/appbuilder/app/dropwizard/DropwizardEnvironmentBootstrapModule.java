package net.spals.appbuilder.app.dropwizard;

import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import io.dropwizard.setup.Environment;

import javax.validation.Validator;

/**
 * A {@link BootstrapModule} for binding
 * common Dropwizard {@link Environment} state.
 *
 * @author tkral
 */
class DropwizardEnvironmentBootstrapModule implements BootstrapModule {

    private final Environment env;

    DropwizardEnvironmentBootstrapModule(final Environment env) {
        this.env = env;
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        bootstrapBinder.bind(Environment.class).toInstance(env);
        bootstrapBinder.bind(Validator.class).toInstance(env.getValidator());
    }
}
