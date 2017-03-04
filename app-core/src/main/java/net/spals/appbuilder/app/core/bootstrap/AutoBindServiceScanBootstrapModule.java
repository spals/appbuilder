package net.spals.appbuilder.app.core.bootstrap;

import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import net.spals.appbuilder.annotations.config.ServiceScan;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link BootstrapModule} which binds
 * the {@link Reflections} object which
 * represents the dynamic service scan.
 *
 * @author tkral
 */
public class AutoBindServiceScanBootstrapModule implements BootstrapModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindServiceScanBootstrapModule.class);

    private final Reflections serviceScan;

    public AutoBindServiceScanBootstrapModule(final Reflections serviceScan) {
        this.serviceScan = serviceScan;
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        bootstrapBinder.bind(Reflections.class).annotatedWith(ServiceScan.class).toInstance(serviceScan);
    }
}
