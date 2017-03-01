package net.spals.appbuilder.app;

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
import net.spals.appbuilder.annotations.config.ApplicationName;
import net.spals.appbuilder.app.bootstrap.AutoBindConfigBootstrapModule;
import net.spals.appbuilder.app.bootstrap.AutoBindModulesBootstrapModule;
import net.spals.appbuilder.app.modules.AutoBindMigrationsModule;
import net.spals.appbuilder.app.modules.AutoBindServicesModule;
import net.spals.appbuilder.app.modules.AutoBindWebServerModule;
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

        public App build() {
            setLifecycleInjector(lifecycleInjectorBuilder.build());
            return super.build();
        }
    }

}
