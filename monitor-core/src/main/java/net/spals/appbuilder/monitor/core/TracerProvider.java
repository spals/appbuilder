package net.spals.appbuilder.monitor.core;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.ConfigException;
import io.opentracing.Tracer;
import net.spals.appbuilder.annotations.service.AutoBindProvider;

import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindProvider
class TracerProvider implements Provider<Tracer> {

    @Configuration("tracing.system")
    private volatile String tracingSystem = "noop";

    private final Map<String, TracerPlugin> tracerPluginMap;

    @Inject
    TracerProvider(final Map<String, TracerPlugin> tracerPluginMap) {
        this.tracerPluginMap = tracerPluginMap;
    }

    @Override
    public Tracer get() {
        return Optional.ofNullable(tracerPluginMap.get(tracingSystem))
            .map(tracerPlugin -> tracerPlugin.createTracer())
            .orElseThrow(() -> new ConfigException.BadValue("tracing.system",
                "No Tracing plugin found for : " + tracingSystem));
    }
}
