package net.spals.appbuilder.app.core.generic;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.transformer.OverrideAllDuplicateBindings;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.WorkerAppBuilder;
import net.spals.appbuilder.app.core.bootstrap.AutoBindModulesBootstrapModule;
import net.spals.appbuilder.app.core.bootstrap.BootstrapModuleWrapper;
import net.spals.appbuilder.app.core.modules.AutoBindConfigModule;
import net.spals.appbuilder.app.core.modules.AutoBindServiceGraphModule;
import net.spals.appbuilder.app.core.modules.AutoBindServicesModule;
import net.spals.appbuilder.config.provider.TypesafeConfigurationProvider;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.writer.ServiceGraphWriter;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class GenericWorkerApp implements App {

    public static class Builder extends GenericWorkerApp_Builder implements WorkerAppBuilder<GenericWorkerApp> {

        private final LifecycleInjectorBuilder lifecycleInjectorBuilder;
        private final ServiceGraph serviceGraph = new ServiceGraph();

        private final AutoBindConfigModule.Builder configModuleBuilder;
        private final AutoBindServiceGraphModule.Builder serviceGraphModuleBuilder;
        private final AutoBindServicesModule.Builder servicesModuleBuilder =
                new AutoBindServicesModule.Builder();

        public Builder(final String name, final Logger logger) {
            this.lifecycleInjectorBuilder = LifecycleInjector.builder()
                    .ignoringAllAutoBindClasses()
                    .withBootstrapModule(bootstrapBinder -> {
                        bootstrapBinder.disableAutoBinding();
                        bootstrapBinder.requireExactBindingAnnotations();
                    });
            this.configModuleBuilder = new AutoBindConfigModule.Builder(name);
            this.serviceGraphModuleBuilder = new AutoBindServiceGraphModule.Builder(serviceGraph);

            setName(name);
            setLogger(logger);
            setServiceConfig(ConfigFactory.empty());
        }

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
        public Builder enableBindingOverrides() {
            lifecycleInjectorBuilder.withModuleTransformer(new OverrideAllDuplicateBindings());
            return this;
        }

        @Override
        public Builder enableServiceGraph(final ServiceGraphFormat graphFormat) {
            serviceGraphModuleBuilder.setGraphFormat(graphFormat);
            return this;
        }

        public ServiceGraph getServiceGraph() {
            return serviceGraph;
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
        public Builder setServiceScan(final ServiceScan serviceScan) {
            // 1. Bind the serviceScan as part of the overall application configuration
            configModuleBuilder.setServiceScan(serviceScan);
            // 2. Use the serviceScan to find auto bound modules
            addBootstrapModule(new AutoBindModulesBootstrapModule(serviceScan));
            // 3. Use the serviceScan to find auto bound services
            servicesModuleBuilder.setServiceScan(serviceScan);

            return this;
        }

        @Override
        public GenericWorkerApp build() {
            final AutoBindConfigModule configModule = configModuleBuilder.build();
            addBootstrapModule(bootstrapBinder ->
                // This will parse the configuration and deliver its individual pieces
                // to @Configuration fields.
                bootstrapBinder.bindConfigurationProvider()
                    .toInstance(new TypesafeConfigurationProvider(configModule.getServiceConfig())));

            // Add config and serviceGraph bindings in bootstrap phase
            // so that they can be consumed by auto bound Modules
            addBootstrapModule(new BootstrapModuleWrapper(configModule));
            addBootstrapModule(new BootstrapModuleWrapper(serviceGraphModuleBuilder.build()));
            addModule(servicesModuleBuilder.build());

            setServiceInjector(buildServiceInjector(lifecycleInjectorBuilder));
            return super.build();
        }

        private Injector buildServiceInjector(final LifecycleInjectorBuilder lifecycleInjectorBuilder) {
            // 1. Startup the Governator LifecycleManager
            final LifecycleInjector lifecycleInjector = lifecycleInjectorBuilder.build();
            final LifecycleManager lifecycleManager = lifecycleInjector.getLifecycleManager();
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
            final Injector serviceInjecter = lifecycleInjector.createInjector();
            // 4. Output the service graph
            final ServiceGraphWriter serviceGraphWriter = serviceInjecter.getInstance(Key.get(ServiceGraphWriter.class));
            serviceGraphWriter.writeServiceGraph();

            return serviceInjecter;
        }
    }

}
