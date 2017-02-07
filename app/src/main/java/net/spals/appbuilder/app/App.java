package net.spals.appbuilder.app;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import io.dropwizard.setup.Environment;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.app.bootstrap.AutoBindModulesBootstrapModule;
import net.spals.appbuilder.app.modules.AutoBindJerseyModule;
import net.spals.appbuilder.app.modules.AutoBindServicesModule;
import net.spals.appbuilder.config.provider.TypesafeConfigurationProvider;
import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.TracingConfig;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class App {

    public abstract Logger getLogger();

    abstract LifecycleInjector getLifecycleInjector();

    public abstract String getName();

    public abstract Config getServiceConfig();

    public final Injector getServiceInjector() throws Exception {
        final LifecycleManager lifecycleManager = getLifecycleInjector().getLifecycleManager();
        lifecycleManager.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            getLogger().info("Shutting down {} application.", getName());
            lifecycleManager.close();
        }));
        return getLifecycleInjector().createInjector();
    }

    public static class Builder extends App_Builder {

        private final LifecycleInjectorBuilder lifecycleInjectorBuilder;
        private Optional<Reflections> serviceScan = Optional.empty();

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

        public Builder enableApiAutoBinding(final ResourceConfig jerseyConfig) {
            return addModule(new AutoBindJerseyModule(jerseyConfig));
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

        public Builder enableRequestScoping(final BiFunction<String, Filter, FilterRegistration.Dynamic> filterRegistration) {
            filterRegistration.apply(GuiceFilter.class.getName(), new GuiceFilter())
                    .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false /*isMatchAfter*/, "/*");
            return addModule(new ServletModule());
        }

        public Builder setServiceConfigFromClasspath(final String serviceConfigFileName) {
            return setServiceConfig(ConfigFactory.load(serviceConfigFileName,
                    ConfigParseOptions.defaults().setAllowMissing(false),
                    ConfigResolveOptions.defaults()));
        }

        public Builder setServiceScan(final Reflections serviceScan) {
            this.serviceScan = Optional.ofNullable(serviceScan);
            return this;
        }

        public Builder usingDropwizard(final Environment env) {
            // Bind standard DropWizard state
            addBootstrapModule(bootstrapBinder -> {
                bootstrapBinder.bind(Environment.class).toInstance(env);
                bootstrapBinder.bind(HealthCheckRegistry.class).toInstance(env.healthChecks());
                bootstrapBinder.bind(MetricRegistry.class).toInstance(env.metrics());
            });

            enableApiAutoBinding(env.jersey().getResourceConfig());
            enableRequestScoping((filterName, filter) -> env.servlets().addFilter(filterName, filter));
            return setName(env.getName());
        }

        public App build() {
            // Bind the service configuration
            lifecycleInjectorBuilder.withAdditionalBootstrapModules(bootstrapBinder -> {
                // This will parse the configuration and deliver its individual pieces
                // to @Configuration fields.
                bootstrapBinder.bindConfigurationProvider()
                        .toInstance(new TypesafeConfigurationProvider(getServiceConfig()));
                // Also give access to the full blown configuration
                bootstrapBinder.bind(Config.class).annotatedWith(ServiceConfig.class).toInstance(getServiceConfig());
            });

            serviceScan.ifPresent(scan -> lifecycleInjectorBuilder
                .withAdditionalBootstrapModules(new AutoBindModulesBootstrapModule(scan))
                .withAdditionalModules(new AutoBindServicesModule(scan)));
            setLifecycleInjector(lifecycleInjectorBuilder.build());
            return super.build();
        }
    }

}
