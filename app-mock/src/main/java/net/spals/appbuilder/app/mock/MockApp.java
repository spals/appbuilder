package net.spals.appbuilder.app.mock;

import com.google.inject.Module;
import com.typesafe.config.Config;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.WorkerAppBuilder;
import net.spals.appbuilder.app.core.generic.GenericWorkerApp;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link App} implementation that allows a
 * caller to override scanned services with mocked
 * services. In other words, this will execute a service scan
 * and selectively replace services within the dependency
 * graph with mocked services.
 * <p/>
 * This is a valuable pattern as it allows a
 * mixture of real and mocked services for testing purposes.
 *
 * @author tkral
 */
@FreeBuilder
public abstract class MockApp implements App {

    public static class Builder extends MockApp_Builder implements WorkerAppBuilder<MockApp> {

        private final GenericWorkerApp.Builder appDelegateBuilder;
        private final List<Module> mockSingletonModules = new ArrayList<>();

        public Builder(final Class<?> testClazz) {
            this.appDelegateBuilder =
                new GenericWorkerApp.Builder(testClazz.getName(), LoggerFactory.getLogger(testClazz));
            // Always enable binding overrides so that mocked services
            // can override real ones.
            appDelegateBuilder.enableBindingOverrides();
        }

        public <I> Builder addMockSingleton(final MockSingleton<I> mockSingleton) {
            final Module mockSingletonModule =
                binder -> binder.bind(mockSingleton.baseClass()).toInstance((I) mockSingleton);
            mockSingletonModules.add(mockSingletonModule);
            return this;
        }

        public <I, M extends I> Builder addMockSingleton(final M mockedService, final Class<I> baseClass) {
            final Module mockSingletonModule = binder -> binder.bind(baseClass).toInstance(mockedService);
            mockSingletonModules.add(mockSingletonModule);
            return this;
        }

        @Override
        public Builder addModule(final Module module) {
            appDelegateBuilder.addModule(module);
            return this;
        }

        @Override
        public Builder disableErrorOnServiceLeaks() {
            appDelegateBuilder.disableErrorOnServiceLeaks();
            return this;
        }

        @Override
        public Builder enableBindingOverrides() {
            // Binding overrides are always enabled
            return this;
        }

        @Override
        public Builder enableServiceGraph(final ServiceGraphFormat graphFormat) {
            appDelegateBuilder.enableServiceGraph(graphFormat);
            return this;
        }

        @Override
        public Builder setServiceConfig(final Config serviceConfig) {
            appDelegateBuilder.setServiceConfig(serviceConfig);
            return this;
        }

        @Override
        public Builder setServiceConfigFromClasspath(final String serviceConfigFileName) {
            appDelegateBuilder.setServiceConfigFromClasspath(serviceConfigFileName);
            return this;
        }

        @Override
        public Builder setServiceScan(final ServiceScan serviceScan) {
            appDelegateBuilder.setServiceScan(serviceScan);
            return this;
        }

        @Override
        public MockApp build() {
            // Add all mocked services as the last module so that the
            // mocks are guaranteed to be added to the dependency graph.
            mockSingletonModules.forEach(mockSingletonModule -> appDelegateBuilder.addModule(mockSingletonModule));

            final GenericWorkerApp appDelegate = appDelegateBuilder.build();
            super.setLogger(appDelegate.getLogger());
            super.setName(appDelegate.getName());
            super.setServiceConfig(appDelegate.getServiceConfig());
            super.setServiceInjector(appDelegate.getServiceInjector());

            return super.build();
        }
    }
}
