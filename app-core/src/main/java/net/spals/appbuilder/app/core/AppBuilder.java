package net.spals.appbuilder.app.core;

import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapModule;
import com.typesafe.config.Config;
import net.spals.appbuilder.app.core.grapher.ServiceGrapher;
import org.reflections.Reflections;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.ws.rs.core.Configurable;
import java.io.File;
import java.util.function.BiFunction;

/**
 * @author tkral
 */
public interface AppBuilder<A extends App> {

    AppBuilder<A> addBootstrapModule(BootstrapModule bootstrapModule);

    AppBuilder<A> addModule(Module module);

    AppBuilder<A> disableErrorOnServiceLeaks();

    AppBuilder<A> enableRequestScoping(BiFunction<String, Filter, FilterRegistration.Dynamic> filterRegistration);

    AppBuilder<A> enableServiceGrapher(ServiceGrapher.Type serviceGrapherType);

    AppBuilder<A> enableWebServerAutoBinding(Configurable<?> configurable);

    AppBuilder<A> setLogger(Logger logger);

    AppBuilder<A> setName(String name);

    AppBuilder<A> setServiceConfig(Config serviceConfig);

    AppBuilder<A> setServiceConfigFromClasspath(String serviceConfigFileName);

    AppBuilder<A> setServiceScan(Reflections serviceScan);

    A build();
}
