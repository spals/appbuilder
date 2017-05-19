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

    WorkerAppBuilder<A> addModule(Module module);

    WorkerAppBuilder<A> disableErrorOnServiceLeaks();

    @Beta
    WorkerAppBuilder<A> enableServiceGraph(ServiceGraphFormat graphFormat);

    WorkerAppBuilder<A> setServiceConfig(Config serviceConfig);

    WorkerAppBuilder<A> setServiceConfigFromClasspath(String serviceConfigFileName);

    WorkerAppBuilder<A> setServiceScan(ServiceScan serviceScan);

    A build();
}
