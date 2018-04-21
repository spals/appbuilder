package net.spals.appbuilder.app.grpc.sample;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;

/**
 * A sample guice {@link Module} for
 * {@link net.spals.appbuilder.app.grpc.GrpcWebApp.Builder#addModule(Module)}
 *
 * @author tkral
 */
class SampleGrpcGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        binder().bind(String.class).annotatedWith(Names.named("GuiceModule"))
            .toInstance(this.getClass().getSimpleName());
    }
}
