package net.spals.appbuilder.app.core.sample;

import com.google.inject.Module;
import com.google.inject.name.Names;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

/**
 * A sample guice {@link BootstrapModule} for
 * {@link net.spals.appbuilder.app.dropwizard.DropwizardWebApp.Builder#addModule(Module)}
 *
 * @author tkral
 */
public class SampleCoreBootstrapModule implements BootstrapModule {

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        bootstrapBinder.bind(String.class).annotatedWith(Names.named("BootstrapModule"))
                .toInstance(this.getClass().getName());
    }
}
