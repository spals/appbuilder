package net.spals.appbuilder.app.core.sample;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;

/**
 * A sample guice {@link Module}.
 *
 * @author tkral
 */
public class SampleCoreGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        binder().bind(String.class).annotatedWith(Names.named("GuiceModule"))
            .toInstance(this.getClass().getSimpleName());
    }
}
