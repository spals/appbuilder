package net.spals.appbuilder.app.core.bootstrap;

import com.google.inject.Key;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import net.spals.appbuilder.app.core.grapher.ServiceGrapher;
import net.spals.appbuilder.app.core.grapher.ascii.AsciiServiceGrapher;
import net.spals.appbuilder.app.core.grapher.noop.NoOpServiceGrapher;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class AutoBindServiceGrapherBootstrapModule implements BootstrapModule {

    public abstract String getFileName();
    public abstract Logger getLogger();
    public abstract ServiceGrapher.Type getType();

    public abstract ServiceGrapher getServiceGrapher();

    public static class Builder extends AutoBindServiceGrapherBootstrapModule_Builder {
        public Builder() {
            setType(ServiceGrapher.Type.NO_OP);
        }

        @Override
        public Builder setServiceGrapher(final ServiceGrapher serviceGrapher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AutoBindServiceGrapherBootstrapModule build() {
            switch (getType()) {
                case NO_OP:
                    super.setServiceGrapher(new NoOpServiceGrapher(getLogger()));
                    break;
                case ASCII:
                    super.setServiceGrapher(new AsciiServiceGrapher(getLogger()));
                    break;
            }

            return super.build();
        }
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        final Key<ServiceGrapher> serviceGrapherKey = Key.get(ServiceGrapher.class);
        bootstrapBinder.bind(serviceGrapherKey).toInstance(getServiceGrapher());
        getServiceGrapher().addVertex(serviceGrapherKey);
    }
}
