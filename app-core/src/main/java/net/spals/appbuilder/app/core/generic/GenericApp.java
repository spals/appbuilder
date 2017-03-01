package net.spals.appbuilder.app.core.generic;

import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import net.spals.appbuilder.annotations.config.ApplicationName;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.AppBuilder;
import net.spals.appbuilder.app.core.bootstrap.AutoBindConfigBootstrapModule;
import net.spals.appbuilder.app.core.bootstrap.AutoBindModulesBootstrapModule;
import net.spals.appbuilder.app.core.modules.AutoBindMigrationsModule;
import net.spals.appbuilder.app.core.modules.AutoBindServicesModule;
import net.spals.appbuilder.app.core.modules.AutoBindWebServerModule;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.ws.rs.core.Configurable;
import java.util.EnumSet;
import java.util.function.BiFunction;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class GenericApp implements App {

    public static class Builder extends GenericApp_Builder implements AppBuilder<GenericApp> {

        private final LifecycleInjectorBuilder lifecycleInjectorBuilder;

        public Builder() {
            this.lifecycleInjectorBuilder = LifecycleInjector.builder()
                    .ignoringAllAutoBindClasses()
                    .withBootstrapModule(bootstrapBinder -> {
                        bootstrapBinder.disableAutoBinding();
                        bootstrapBinder.requireExactBindingAnnotations();
                    });
        }

        public Builder addBootstrapModule(final BootstrapModule bootstrapModule) {
            lifecycleInjectorBuilder.withAdditionalBootstrapModules(bootstrapModule);
            return this;
        }

        public Builder addModule(final Module module) {
            lifecycleInjectorBuilder.withAdditionalModules(module);
            return this;
        }

        public Builder enableRequestScoping(final BiFunction<String, Filter, FilterRegistration.Dynamic> filterRegistration) {
            filterRegistration.apply(GuiceFilter.class.getName(), new GuiceFilter())
                    .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false /*isMatchAfter*/, "/*");
            return addModule(new ServletModule());
        }

        public Builder enableWebServerAutoBinding(final Configurable<?> configurable) {
            return addModule(new AutoBindWebServerModule(configurable));
        }

        @Override
        public Builder setName(final String name) {
            addBootstrapModule(bootstrapBinder ->
                    bootstrapBinder.bind(String.class).annotatedWith(ApplicationName.class).toInstance(name));
            return super.setName(name);
        }

        @Override
        public Builder setServiceConfig(final Config serviceConfig) {
            addBootstrapModule(new AutoBindConfigBootstrapModule(serviceConfig));
            return super.setServiceConfig(serviceConfig);
        }

        public Builder setServiceConfigFromClasspath(final String serviceConfigFileName) {
            return setServiceConfig(ConfigFactory.load(serviceConfigFileName,
                    ConfigParseOptions.defaults().setAllowMissing(false),
                    ConfigResolveOptions.defaults()));
        }

        public Builder setServiceScan(final Reflections serviceScan) {
            addBootstrapModule(new AutoBindModulesBootstrapModule(serviceScan));
            addModule(new AutoBindMigrationsModule(serviceScan));
            return addModule(new AutoBindServicesModule(serviceScan));
        }

        public GenericApp build() {
            setLifecycleInjector(lifecycleInjectorBuilder.build());
            return super.build();
        }
    }

}
