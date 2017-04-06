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
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.AppBuilder;
import net.spals.appbuilder.app.core.bootstrap.AutoBindConfigBootstrapModule;
import net.spals.appbuilder.app.core.bootstrap.AutoBindModulesBootstrapModule;
import net.spals.appbuilder.app.core.bootstrap.AutoBindServiceGrapherBootstrapModule;
import net.spals.appbuilder.app.core.modules.AutoBindServicesModule;
import net.spals.appbuilder.app.core.modules.AutoBindWebServerModule;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;
import org.slf4j.Logger;

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

        private final AutoBindConfigBootstrapModule.Builder configModuleBuilder =
                new AutoBindConfigBootstrapModule.Builder();
        private final AutoBindServiceGrapherBootstrapModule.Builder serviceGrapherModuleBuilder =
                new AutoBindServiceGrapherBootstrapModule.Builder();
        private final AutoBindServicesModule.Builder servicesModuleBuilder =
                new AutoBindServicesModule.Builder();
        private final AutoBindWebServerModule.Builder webServerModuleBuilder =
                new AutoBindWebServerModule.Builder();

        public Builder() {
            this.lifecycleInjectorBuilder = LifecycleInjector.builder()
                    .ignoringAllAutoBindClasses()
                    .withBootstrapModule(bootstrapBinder -> {
                        bootstrapBinder.disableAutoBinding();
                        bootstrapBinder.requireExactBindingAnnotations();
                    });
        }

        @Override
        public Builder addBootstrapModule(final BootstrapModule bootstrapModule) {
            lifecycleInjectorBuilder.withAdditionalBootstrapModules(bootstrapModule);
            return this;
        }

        @Override
        public Builder addModule(final Module module) {
            lifecycleInjectorBuilder.withAdditionalModules(module);
            return this;
        }

        @Override
        public Builder disableErrorOnServiceLeaks() {
            servicesModuleBuilder.setErrorOnServiceLeaks(false);
            return this;
        }

        @Override
        public Builder enableRequestScoping(final BiFunction<String, Filter, FilterRegistration.Dynamic> filterRegistration) {
            filterRegistration.apply(GuiceFilter.class.getName(), new GuiceFilter())
                    .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false /*isMatchAfter*/, "/*");
            return addModule(new ServletModule());
        }

        @Override
        public Builder enableServiceGrapher(final ServiceGraphFormat graphFormat) {
            serviceGrapherModuleBuilder.setGraphFormat(graphFormat);
            return this;
        }

        @Override
        public Builder enableWebServerAutoBinding(final Configurable<?> configurable) {
            webServerModuleBuilder.setConfigurable(configurable);
            return this;
        }

        @Override
        public Builder setLogger(final Logger logger) {
            serviceGrapherModuleBuilder.setLogger(logger);
            return super.setLogger(logger);
        }

        @Override
        public Builder setName(final String name) {
            configModuleBuilder.setApplicationName(name);
            serviceGrapherModuleBuilder.setFileName(name + "-services-graph");
            return super.setName(name);
        }

        @Override
        public Builder setServiceConfig(final Config serviceConfig) {
            configModuleBuilder.setServiceConfig(serviceConfig);
            return super.setServiceConfig(serviceConfig);
        }

        @Override
        public Builder setServiceConfigFromClasspath(final String serviceConfigFileName) {
            return setServiceConfig(ConfigFactory.load(serviceConfigFileName,
                    ConfigParseOptions.defaults().setAllowMissing(false),
                    ConfigResolveOptions.defaults()));
        }

        @Override
        public Builder setServiceScan(final Reflections serviceScan) {
            // 1. Bind the serviceScan as part of the overall application configuration
            configModuleBuilder.setServiceScan(serviceScan);
            // 2. Use the serviceScan to find auto bound modules
            addBootstrapModule(new AutoBindModulesBootstrapModule(serviceScan));
            // 3. Use the serviceScan to find auto bound services
            servicesModuleBuilder.setServiceScan(serviceScan);

            return this;
        }

        @Override
        public GenericApp build() {
            final AutoBindServiceGrapherBootstrapModule serviceGrapherModule = serviceGrapherModuleBuilder.build();
            addBootstrapModule(serviceGrapherModule);

//            configModuleBuilder.setServiceGrapher(serviceGrapherModule.getServiceGrapher());
            addBootstrapModule(configModuleBuilder.build());

//            servicesModuleBuilder.setServiceGrapher(serviceGrapherModule.getServiceGrapher());
            addModule(servicesModuleBuilder.build());

            webServerModuleBuilder.setServiceGraph(serviceGrapherModule.getServiceGraph());
            addModule(webServerModuleBuilder.build());

            setLifecycleInjector(lifecycleInjectorBuilder.build());
            return super.build();
        }
    }

}
