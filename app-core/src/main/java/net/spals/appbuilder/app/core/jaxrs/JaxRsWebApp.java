package net.spals.appbuilder.app.core.jaxrs;

import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import com.netflix.governator.guice.BootstrapModule;
import com.typesafe.config.Config;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.WebAppBuilder;
import net.spals.appbuilder.app.core.generic.GenericWorkerApp;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.inferred.freebuilder.FreeBuilder;
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
public abstract class JaxRsWebApp implements App {

    public abstract Configurable<?> getConfigurable();
    public abstract BiFunction<String, Filter, FilterRegistration.Dynamic> getFilterRegistration();

    public static class Builder extends JaxRsWebApp_Builder implements WebAppBuilder<JaxRsWebApp> {

        private final GenericWorkerApp.Builder appDelegateBuilder;
        private final JaxRsCorsModule.Builder corsModuleBuilder =
            new JaxRsCorsModule.Builder();
        private final JaxRsDocModule.Builder docModuleBuilder =
            new JaxRsDocModule.Builder();
        private final JaxRsMonitorModule.Builder monitorModuleBuilder =
            new JaxRsMonitorModule.Builder();
        private final JaxRsWebServerModule.Builder webServerModuleBuilder =
            new JaxRsWebServerModule.Builder();

        public Builder(final String name, final Logger logger) {
            this.appDelegateBuilder = new GenericWorkerApp.Builder(name, logger);
            docModuleBuilder.setApplicationName(name);
        }

        public Builder addBootstrapModule(final BootstrapModule bootstrapModule) {
            appDelegateBuilder.addBootstrapModule(bootstrapModule);
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
        public Builder disableWebServerAutoBinding() {
            docModuleBuilder.setWebServerAutoBindingEnabled(false);
            webServerModuleBuilder.setWebServerAutoBindingEnabled(false);
            return this;
        }

        @Override
        public Builder enableBindingOverrides() {
            appDelegateBuilder.enableBindingOverrides();
            return this;
        }

        @Override
        public Builder enableCors() {
            corsModuleBuilder.setCorsEnabled(true);
            return this;
        }

        @Override
        public Builder enableRequestScoping() {
            // TODO: Potential error here. If .enableRequestScoping() is called before .setFilterRegistration(...)
            getFilterRegistration().apply(GuiceFilter.class.getName(), new GuiceFilter())
                    .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false /*isMatchAfter*/, "/*");
            return addModule(new ServletModule());
        }

        @Override
        public Builder enableServiceGraph(final ServiceGraphFormat graphFormat) {
            appDelegateBuilder.enableServiceGraph(graphFormat);
            return this;
        }

        @Override
        public Builder setConfigurable(final Configurable<?> configurable) {
            corsModuleBuilder.setConfigurable(configurable);
            docModuleBuilder.setConfigurable(configurable);
            monitorModuleBuilder.setConfigurable(configurable);
            webServerModuleBuilder.setConfigurable(configurable);
            return super.setConfigurable(configurable);
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
            docModuleBuilder.setServiceScan(serviceScan);
            return this;
        }

        @Override
        public JaxRsWebApp build() {
            appDelegateBuilder.addModule(corsModuleBuilder.build());
            appDelegateBuilder.addModule(docModuleBuilder.build());
            appDelegateBuilder.addModule(monitorModuleBuilder.build());

            webServerModuleBuilder.setServiceGraph(appDelegateBuilder.getServiceGraph());
            appDelegateBuilder.addModule(webServerModuleBuilder.build());

            final GenericWorkerApp appDelegate = appDelegateBuilder.build();
            super.setLogger(appDelegate.getLogger());
            super.setName(appDelegate.getName());
            super.setServiceConfig(appDelegate.getServiceConfig());
            super.setServiceInjector(appDelegate.getServiceInjector());

            return super.build();
        }
    }
}
