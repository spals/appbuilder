package net.spals.appbuilder.app.core;

import com.google.common.annotations.Beta;
import com.google.inject.Module;
import com.typesafe.config.Config;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import org.reflections.Reflections;

/**
 * Contract definition for a builder which produces
 * an instance of a simple {@link App}.
 *
 * A simple app is defined as an application which
 * does not requires a web server.
 *
 * For applications requiring a web server, see
 * {@link WebAppBuilder}.
 *
 * @author tkral
 */
public interface SimpleAppBuilder<A extends App> {

    SimpleAppBuilder<A> addModule(Module module);

    SimpleAppBuilder<A> disableErrorOnServiceLeaks();

    @Beta
    SimpleAppBuilder<A> enableServiceGraph(ServiceGraphFormat graphFormat);

    SimpleAppBuilder<A> setServiceConfig(Config serviceConfig);

    SimpleAppBuilder<A> setServiceConfigFromClasspath(String serviceConfigFileName);

    SimpleAppBuilder<A> setServiceScan(Reflections serviceScan);

    A build();
}
