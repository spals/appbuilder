package net.spals.appbuilder.config.service;

import com.google.common.base.Predicates;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.Set;

/**
 * Bean for holding configuration for a service scan.
 *
 * This informs an application of where to look for
 * services to automatically bind.
 *
 * This is a simple wrapper around a {@link Reflections}
 * object with syntactic sugar for adding service packages
 * to the scan.
 *
 * @author tkral
 */
@FreeBuilder
public interface ServiceScan {

    Set<String> getServicePackages();

    Reflections getReflections();

    static ServiceScan empty() {
        return new ServiceScan.Builder().build();
    }

    class Builder extends ServiceScan_Builder {

        private static Reflections EMPTY_REFLECTIONS = new Reflections(Predicates.alwaysFalse());

        public Builder() {
            setReflections(EMPTY_REFLECTIONS);
            // Executor and monitoring services are special because they
            // are used widely throughout the framework in several modules.
            // As such, the service scan will guarantee that core executor
            // and monitoring services are always injected without the
            // application author having to do so manually. However, they
            // will have to install plugins manually
            addServicePackages(
                "net.spals.appbuilder.executor.core",
                "net.spals.appbuilder.monitor.core"
            );
        }

        public Builder addDefaultServices(final Class<?> serviceClass) {
            return addServicePackages(serviceClass.getPackage().getName());
        }

        public Builder addDefaultServices(final Class<?>... serviceClasses) {
            return addAllDefaultServices(Arrays.asList(serviceClasses));
        }

        public Builder addAllDefaultServices(final Iterable<Class<?>> serviceClasses) {
            serviceClasses.forEach(this::addDefaultServices);
            return this;
        }

        public Builder addServicePlugins(final String pluginPackage, final Class<?> serviceClass) {
            addDefaultServices(serviceClass);
            return addServicePackages(pluginPackage);
        }

        public Builder addServicePlugins(final String pluginPackage, final Class<?>... serviceClasses) {
            addDefaultServices(serviceClasses);
            return addServicePackages(pluginPackage);
        }

        public Builder addAllServicePlugins(final String pluginPackage, final Iterable<Class<?>> serviceClasses) {
            addAllDefaultServices(serviceClasses);
            return addServicePackages(pluginPackage);
        }

        @Override
        public ServiceScan build() {
            // Honor any Reflections object which is directly injected into the builder.
            // This is most useful for testing purposes.
            if (getReflections() == EMPTY_REFLECTIONS && !getServicePackages().isEmpty()) {
                setReflections(new Reflections(getServicePackages().stream()
                        .toArray(String[]::new)));
            }

            return super.build();
        }
    }
}
