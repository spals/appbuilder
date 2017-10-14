package net.spals.appbuilder.config.service;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binding;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import net.spals.appbuilder.config.matcher.TypeLiteralMatchers;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.Set;

import static com.google.inject.matcher.Matchers.inSubpackage;
import static net.spals.appbuilder.config.matcher.BindingMatchers.keyTypeThat;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.hasParameterTypeThat;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;

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

    default Matcher<Binding<?>> asBindingMatcher() {
        return keyTypeThat(asTypeLiteralMatcher());
    }

    default Matcher<TypeLiteral<?>> asTypeLiteralMatcher() {
        return getServicePackages().stream()
            .map(servicePackage -> rawTypeThat(inSubpackage(servicePackage))
                .or(hasParameterTypeThat(rawTypeThat(inSubpackage(servicePackage)))))
            .reduce(TypeLiteralMatchers.none(), (matcher1, matcher2) -> matcher1.or(matcher2));
    }

    Set<String> getServicePackages();

    Reflections getReflections();

    static ServiceScan empty() {
        return new ServiceScan.Builder()
            // Provide an empty Reflections instance. We do this here
            // because we don't want the empty ServiceScan to include
            // any default packages (e.g. executor and monitor core packages).
            // So this circumvents the UNSET_REFLECTIONS check but
            // still has the same effect.
            .setReflections(new Reflections(Predicates.alwaysFalse()))
            .build();
    }

    class Builder extends ServiceScan_Builder {

        // Default service packages are special because they are used
        // widely throughout the framework in several modules.
        //
        // As such, the service scan will guarantee that they
        // are always injected without the application author
        // having to do so explicitly.
        //
        // However, default service packages will be skipped if
        // the caller provides a Reflections instance directly.
        // Also, plugins to these services have to be explicitly
        // added.
        //
        // *IMPORTANT NOTE*: Any packages added here should also
        // be added as runtime-scoped dependencies in the app-core
        // module to guarantee that they are always available.
        private static Set<String> DEFAULT_SERVICE_PACKAGES = ImmutableSet.of(
            "net.spals.appbuilder.config",
            "net.spals.appbuilder.executor.core",
            "net.spals.appbuilder.monitor.core"
        );

        // This is a special marker which represents the fact
        // that the caller has not provided a Reflections instance
        // via the setReflections setter. This is important to know
        // in the build method because we'll honor any Reflections
        // instance provided by the caller. Otherwise, we'll create
        // a new one based on the service packages provided.
        private static Reflections UNSET_REFLECTIONS =
                new Reflections(Predicates.alwaysFalse());

        public Builder() {
            setReflections(UNSET_REFLECTIONS);
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
            if (getReflections() == UNSET_REFLECTIONS) {
                // Guarantee that default service packages are added
                addAllServicePackages(DEFAULT_SERVICE_PACKAGES);
                setReflections(new Reflections(getServicePackages().stream()
                        .toArray(String[]::new)));
            }

            return super.build();
        }
    }
}
