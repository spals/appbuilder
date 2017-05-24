package net.spals.appbuilder.app.core;

import com.google.common.annotations.Beta;
import com.google.inject.Module;
import com.typesafe.config.Config;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.reflections.Reflections;

/**
 * Contract definition for a builder which produces
 * an instance of a worker {@link App}.
 *
 * A worker app is defined as an application which
 * does not requires a web server.
 *
 * For applications requiring a web server, see
 * {@link WebAppBuilder}.
 *
 * @author tkral
 */
public interface WorkerAppBuilder<A extends App> {

    /**
     * Add a custom Guice {@link Module} to configure
     * a portion of the application.
     *
     * It is preferred to use binding annotations
     * found within the annotations module as these
     * are easier to managed during development.
     *
     * However, there may be some cases in which
     * a custom {@link Module} is necessary and so
     * this method can be used.
     */
    WorkerAppBuilder<A> addModule(Module module);

    /**
     * Disable application exit when a service leak
     * is detected.
     *
     * Service leak detection enforces proper service
     * design by ensuring that service implementations
     * are not globally visible.
     *
     * By default, when a service leak is detected,
     * the application will fail to boot until the leak
     * is fixed. This feature can be turned off using
     * this method. It is *STRONGLY* recommended that
     * this method is not used.
     */
    WorkerAppBuilder<A> disableErrorOnServiceLeaks();

    @Beta
    WorkerAppBuilder<A> enableServiceGraph(ServiceGraphFormat graphFormat);

    /**
     * Add the full service configuration to the application state.
     *
     * All configuration values found with the {@link Config} object
     * with be available for injection into any auto-bound application
     * services.
     */
    WorkerAppBuilder<A> setServiceConfig(Config serviceConfig);

    /**
     * Add the full service configuration to the application state
     * from a file on the application's classpath.
     *
     * Note that this has the same semantics as {@link #setServiceConfig(Config)}.
     */
    WorkerAppBuilder<A> setServiceConfigFromClasspath(String serviceConfigFileName);

    /**
     * Add a {@link ServiceScan} to the application state.
     */
    WorkerAppBuilder<A> setServiceScan(ServiceScan serviceScan);

    /**
     * Build the application state and return an
     * immutable {@link App} object.
     */
    A build();
}
