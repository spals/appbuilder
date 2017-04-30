package net.spals.appbuilder.app.dropwizard;

import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapModule;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.app.core.WebAppBuilder;
import net.spals.appbuilder.app.core.generic.JaxRsWebApp;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.TracingConfig;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A wrapper for {@link App} which
 * uses Dropwizard as the application
 * framework.
 *
 * @author tkral
 */
@FreeBuilder
public abstract class DropwizardWebApp implements App {

    abstract Bootstrap<?> getBootstrap();
    public abstract Environment getEnvironment();

    public static class Builder extends DropwizardWebApp_Builder implements WebAppBuilder<DropwizardWebApp> {

        private final JaxRsWebApp.Builder appDelegateBuilder;

        public Builder(final Bootstrap<?> bootstrap, final Logger logger) {
            this.appDelegateBuilder = new JaxRsWebApp.Builder(bootstrap.getApplication().getName(), logger);
            setBootstrap(bootstrap);
            addBootstrapModule(new DropwizardBootstrapBootstrapModule(bootstrap));
        }

        @Override
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
            appDelegateBuilder.disableWebServerAutoBinding();
            return this;
        }

        private Builder enableApiRequestTracing(final Environment env) {
            // See documentation at https://jersey.java.net/documentation/latest/monitoring_tracing.html
            env.jersey().property(ServerProperties.TRACING, TracingConfig.ON_DEMAND.toString());
            env.jersey().property(ServerProperties.TRACING_THRESHOLD, TracingLogger.Level.TRACE.toString());

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
        public Builder enableRequestScoping() {
            appDelegateBuilder.enableRequestScoping();
            return this;
        }

        @Override
        public Builder enableServiceGraph(final ServiceGraphFormat graphFormat) {
            appDelegateBuilder.enableServiceGraph(graphFormat);
            return this;
        }

        @Override
        public Builder setEnvironment(final Environment env) {
            appDelegateBuilder.setConfigurable(env.jersey().getResourceConfig());
            appDelegateBuilder.setFilterRegistration((filterName, filter) -> env.servlets().addFilter(filterName, filter));

            addBootstrapModule(new DropwizardEnvironmentBootstrapModule(env));
            enableApiRequestTracing(env);

            return this;
        }

        @Override
        public Builder setServiceConfigFromClasspath(final String serviceConfigFileName) {
            // Create a ConfigurationSourceProvider which reads from the classpath
            final ConfigurationSourceProvider classpathConfigSourceProvider = path ->
                    getBootstrap().getApplication().getClass().getClassLoader().getResourceAsStream(path);
            // Enable variable substitution with environment variables
            final ConfigurationSourceProvider envVarConfigSourceProvider =
                    new SubstitutingSourceProvider(classpathConfigSourceProvider,
                            new EnvironmentVariableSubstitutor(false /*strict*/));

            getBootstrap().setConfigurationSourceProvider(envVarConfigSourceProvider);
            appDelegateBuilder.setServiceConfigFromClasspath(serviceConfigFileName);

            return this;
        }

        @Override
        public Builder setServiceScan(final Reflections serviceScan) {
            appDelegateBuilder.setServiceScan(serviceScan);
            return this;
        }

        @Override
        public DropwizardWebApp build() {
            final JaxRsWebApp appDelegate = appDelegateBuilder.build();
            super.setLogger(appDelegate.getLogger());
            super.setName(appDelegate.getName());
            super.setServiceConfig(appDelegate.getServiceConfig());
            super.setServiceInjector(appDelegate.getServiceInjector());

            return super.build();
        }
    }
}
