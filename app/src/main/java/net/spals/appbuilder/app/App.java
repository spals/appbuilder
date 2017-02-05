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
import net.spals.appbuilder.app.bootstrap.AutoBindModulesBootstrapModule;
import net.spals.appbuilder.app.modules.AutoBindJerseyModule;
import net.spals.appbuilder.app.modules.AutoBindServicesModule;
import net.spals.appbuilder.config.provider.TypesafeConfigurationProvider;
import org.inferred.freebuilder.FreeBuilder;
import org.reflections.Reflections;
import org.slf4j.Logger;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.Optional;

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

        public Builder setServiceConfigFromClasspath(final String serviceConfigFileName) {
            return setServiceConfig(ConfigFactory.load(serviceConfigFileName,
                    ConfigParseOptions.defaults().setAllowMissing(false),
                    ConfigResolveOptions.defaults()));
        }

        public Builder setServiceScan(final Reflections serviceScan) {
            this.serviceScan = Optional.of(serviceScan);
            return this;
        }

        public Builder usingDropwizard(final Environment env) {
            // 1. Bind standard DropWizard state
            addBootstrapModule(bootstrapBinder -> {
                bootstrapBinder.bind(Environment.class).toInstance(env);
                bootstrapBinder.bind(HealthCheckRegistry.class).toInstance(env.healthChecks());
                bootstrapBinder.bind(MetricRegistry.class).toInstance(env.metrics());
            });

            // 2. Auto-bind any Jersey components with Jersey
            addModule(new AutoBindJerseyModule(env.jersey().getResourceConfig()));

            // 3. Turn on request / session scoping
            env.servlets().addFilter(GuiceFilter.class.getName(), new GuiceFilter())
                    .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false /*isMatchAfter*/, "/*");
            addModule(new ServletModule());

            return setName(env.getName());
        }

        public App build() {
            lifecycleInjectorBuilder.withAdditionalBootstrapModules(bootstrapBinder ->
                bootstrapBinder.bindConfigurationProvider()
                    .toInstance(new TypesafeConfigurationProvider(getServiceConfig())));

            serviceScan.ifPresent(scan -> lifecycleInjectorBuilder
                .withAdditionalBootstrapModules(new AutoBindModulesBootstrapModule(scan))
                .withAdditionalModules(new AutoBindServicesModule(scan)));
            setLifecycleInjector(lifecycleInjectorBuilder.build());
            return super.build();
        }
    }

}
