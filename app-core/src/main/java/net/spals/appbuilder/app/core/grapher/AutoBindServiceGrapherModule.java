package net.spals.appbuilder.app.core.grapher;

import com.google.inject.AbstractModule;
import net.spals.appbuilder.app.core.grapher.ascii.AsciiServiceGrapher;
import net.spals.appbuilder.app.core.grapher.noop.NoOpServiceGrapher;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class AutoBindServiceGrapherModule extends AbstractModule {

    public abstract String getFileName();
    public abstract Logger getLogger();
    public abstract ServiceGrapher.Type getType();

    public abstract ServiceGrapher getServiceGrapher();

    public static class Builder extends AutoBindServiceGrapherModule_Builder {
        public Builder() {
            setType(ServiceGrapher.Type.NO_OP);
        }

        @Override
        public Builder setServiceGrapher(final ServiceGrapher serviceGrapher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AutoBindServiceGrapherModule build() {
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
    protected void configure() {
        bind(ServiceGrapher.class).toInstance(getServiceGrapher());
    }
}
