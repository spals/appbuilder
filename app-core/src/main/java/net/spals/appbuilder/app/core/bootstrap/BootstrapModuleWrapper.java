package net.spals.appbuilder.app.core.bootstrap;

import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

/**
 * @author tkral
 */
public class BootstrapModuleWrapper implements BootstrapModule {

    private final Module module;

    public BootstrapModuleWrapper(final Module module) {
        this.module = module;
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        module.configure(bootstrapBinder);
    }
}
