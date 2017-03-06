package net.spals.appbuilder.app.dropwizard;

import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapModule;
import com.typesafe.config.Config;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.AppBuilder;
import net.spals.appbuilder.app.core.generic.GenericApp;
import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.TracingConfig;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.ws.rs.core.Configurable;
import java.util.function.BiFunction;

/**
 * A wrapper for {@link App} which
 * uses Dropwizard as the application
 * framework.
 *
 * @author tkral
 */
@FreeBuilder
public abstract class DropwizardApp implements App {

    public static class Builder extends DropwizardApp_Builder implements AppBuilder<DropwizardApp> {

        private final GenericApp.Builder appBuilderDelegate = new GenericApp.Builder();
        private final Bootstrap<?> bootstrap;

        public Builder(final Bootstrap<?> bootstrap) {
            this.bootstrap = bootstrap;
            addBootstrapModule(new DropwizardBootstrapBootstrapModule(bootstrap));
            setName(bootstrap.getApplication().getName());
        }

        @Override
        public Builder addBootstrapModule(final BootstrapModule bootstrapModule) {
            appBuilderDelegate.addBootstrapModule(bootstrapModule);
            return this;
        }

        @Override
        public Builder addModule(final Module module) {
            appBuilderDelegate.addModule(module);
            return this;
        }

        @Override
        public Builder disableErrorOnServiceLeaks() {
            appBuilderDelegate.disableErrorOnServiceLeaks();
            return this;
        }

        public Builder enableApiRequestTracing(final ResourceConfig jerseyConfig) {
            // See documentation at https://jersey.java.net/documentation/latest/monitoring_tracing.html
            jerseyConfig.property(ServerProperties.TRACING, TracingConfig.ON_DEMAND.toString());
            jerseyConfig.property(ServerProperties.TRACING_THRESHOLD, TracingLogger.Level.TRACE.toString());

            // Install the JUL->slf4j bridge as Jersey uses JUL
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            // Setup the slf4j logger to accept TRACE messages
            final ch.qos.logback.classic.Logger tracingLogger =
                    (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.glassfish.jersey.tracing");
            tracingLogger.setLevel(ch.qos.logback.classic.Level.TRACE);

            return this;
        }
        
        @Override
        public Builder enableRequestScoping(final BiFunction<String, Filter, FilterRegistration.Dynamic> filterRegistration) {
            appBuilderDelegate.enableRequestScoping(filterRegistration);
            return this;
        }

        @Override
        public Builder enableWebServerAutoBinding(final Configurable<?> configurable) {
            appBuilderDelegate.enableWebServerAutoBinding(configurable);
            return this;
        }

        @Override
        public Builder setLogger(final Logger logger) {
            appBuilderDelegate.setLogger(logger);
            return this;
        }

        @Override
        public Builder setName(final String name) {
            appBuilderDelegate.setName(name);
            return this;
        }

        @Override
        public Builder setServiceConfig(final Config serviceConfig) {
            appBuilderDelegate.setServiceConfig(serviceConfig);
            return this;
        }

        @Override
        public Builder setServiceConfigFromClasspath(final String serviceConfigFileName) {
            // Create a ConfigurationSourceProvider which reads from the classpath
            final ConfigurationSourceProvider classpathConfigSourceProvider = path ->
                    bootstrap.getApplication().getClass().getClassLoader().getResourceAsStream(path);
            // Enable variable substitution with environment variables
            final ConfigurationSourceProvider envVarConfigSourceProvider =
                    new SubstitutingSourceProvider(classpathConfigSourceProvider,
                            new EnvironmentVariableSubstitutor(false /*strict*/));

            bootstrap.setConfigurationSourceProvider(envVarConfigSourceProvider);
            appBuilderDelegate.setServiceConfigFromClasspath(serviceConfigFileName);

            return this;
        }

        @Override
        public Builder setServiceScan(final Reflections serviceScan) {
            appBuilderDelegate.setServiceScan(serviceScan);
            return this;
        }

        public Builder usingEnvironment(final Environment env) {
            addBootstrapModule(new DropwizardEnvironmentBootstrapModule(env));
            enableApiRequestTracing(env.jersey().getResourceConfig());
            enableRequestScoping((filterName, filter) -> env.servlets().addFilter(filterName, filter));
            enableWebServerAutoBinding(env.jersey().getResourceConfig());

            return this;
        }

        @Override
        public DropwizardApp build() {
            final GenericApp genericApp = appBuilderDelegate.build();
            super.setLogger(genericApp.getLogger());
            super.setLifecycleInjector(genericApp.getLifecycleInjector());
            super.setName(genericApp.getName());
            super.setServiceConfig(genericApp.getServiceConfig());

            return super.build();
        }
    }
}
