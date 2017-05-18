package net.spals.appbuilder.app.core.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import net.spals.appbuilder.annotations.service.AutoBindModule;
import net.spals.appbuilder.config.service.ServiceScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@link BootstrapModule} which installs
 * {@link Module}s marked for auto binding.
 *
 * @author tkral
 */
public class AutoBindModulesBootstrapModule implements BootstrapModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindModulesBootstrapModule.class);

    private final ServiceScan serviceScan;

    public AutoBindModulesBootstrapModule(final ServiceScan serviceScan) {
        this.serviceScan = serviceScan;
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        autoBindModules(bootstrapBinder);
    }

    @VisibleForTesting
    void autoBindModules(final BootstrapBinder bootstrapBinder) {
        final Set<Class<?>> moduleClasses = serviceScan.getReflections()
            .getTypesAnnotatedWith(AutoBindModule.class);
        validateModules(moduleClasses);

        moduleClasses.stream()
            .forEach(moduleClazz -> {
                LOGGER.info("Auto-binding Guice Module during bootstrap: {}", moduleClazz);
                bootstrapBinder.include((Class<? extends Module>) moduleClazz);
            });
    }

    void validateModules(final Set<Class<?>> moduleClasses) {
        final Set<Class<?>> invalidModules = moduleClasses.stream()
                .filter(moduleClazz -> moduleClazz.isInterface() || !Module.class.isAssignableFrom(moduleClazz))
                .collect(Collectors.toSet());
        checkState(invalidModules.isEmpty(),
                "@AutoBindModule can only annotate Module classes: %s", invalidModules);
    }
}
