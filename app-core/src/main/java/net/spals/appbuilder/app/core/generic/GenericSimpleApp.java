package net.spals.appbuilder.app.core.generic;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.SimpleAppBuilder;
import net.spals.appbuilder.app.core.bootstrap.AutoBindConfigBootstrapModule;
import net.spals.appbuilder.app.core.bootstrap.AutoBindModulesBootstrapModule;
import net.spals.appbuilder.app.core.bootstrap.AutoBindServiceGraphBootstrapModule;
import net.spals.appbuilder.app.core.modules.AutoBindServicesModule;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.writer.ServiceGraphWriter;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;
import org.slf4j.Logger;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class GenericSimpleApp implements App {

    abstract LifecycleInjector getLifecycleInjector();

    @Override
    public final Injector getServiceInjector() {
        // 1. Startup the Governator LifecycleManager
        final LifecycleManager lifecycleManager = getLifecycleInjector().getLifecycleManager();
        try {
            lifecycleManager.start();
        } catch (Exception e) {
            getLogger().error("Error during LifecycleManager start", e);
            throw new RuntimeException(e);
        }

        // 2. Ensure that we shut everything down properly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            getLogger().info("Shutting down {} application.", getName());
            lifecycleManager.close();
        }));
        // 3. Grab the Guice injector from which we can get service references
        final Injector serviceInjecter = getLifecycleInjector().createInjector();
        // 4. Output the service graph
        final ServiceGraphWriter serviceGraphWriter = serviceInjecter.getInstance(Key.get(ServiceGraphWriter.class));
        serviceGraphWriter.writeServiceGraph();

        return serviceInjecter;
    }

    public static class Builder extends GenericSimpleApp_Builder implements SimpleAppBuilder<GenericSimpleApp> {

        private final LifecycleInjectorBuilder lifecycleInjectorBuilder;
        final ServiceGraph serviceGraph;

        private final AutoBindConfigBootstrapModule.Builder configModuleBuilder =
                new AutoBindConfigBootstrapModule.Builder();
        private final AutoBindServiceGraphBootstrapModule.Builder serviceGraphModuleBuilder =
                new AutoBindServiceGraphBootstrapModule.Builder();
        private final AutoBindServicesModule.Builder servicesModuleBuilder =
                new AutoBindServicesModule.Builder();

        public Builder(final String name, final Logger logger) {
            this.configModuleBuilder.setApplicationName(name);
            this.lifecycleInjectorBuilder = LifecycleInjector.builder()
                    .ignoringAllAutoBindClasses()
                    .withBootstrapModule(bootstrapBinder -> {
                        bootstrapBinder.disableAutoBinding();
                        bootstrapBinder.requireExactBindingAnnotations();
                    });
            this.serviceGraph = new ServiceGraph();
            this.serviceGraphModuleBuilder.setServiceGraph(serviceGraph);

            setName(name);
            setLogger(logger);
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
        public Builder enableServiceGraph(final ServiceGraphFormat graphFormat) {
            serviceGraphModuleBuilder.setGraphFormat(graphFormat);
            return this;
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
        public GenericSimpleApp build() {
            addBootstrapModule(configModuleBuilder.build());
            addBootstrapModule(serviceGraphModuleBuilder.build());
            addModule(servicesModuleBuilder.build());

            setLifecycleInjector(lifecycleInjectorBuilder.build());
            return super.build();
        }
    }

}
