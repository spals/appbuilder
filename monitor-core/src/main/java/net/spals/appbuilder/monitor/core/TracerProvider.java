package net.spals.appbuilder.monitor.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.ConfigException;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import net.spals.appbuilder.annotations.service.AutoBindProvider;

import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindProvider
class TracerProvider implements Provider<Tracer> {

    @Configuration("tracing.system")
    @VisibleForTesting
    volatile String tracingSystem = "noop";

    private final Map<String, TracerPlugin> tracerPluginMap;
    private final Map<String, TracerTag> tracerTagMap;

    @Inject
    TracerProvider(final Map<String, TracerPlugin> tracerPluginMap,
                   final Map<String, TracerTag> tracerTagMap) {
        this.tracerPluginMap = tracerPluginMap;
        this.tracerTagMap = tracerTagMap;
    }

    @Override
    public Tracer get() {
        // Lookup the tracer from the available plugins
        final Tracer tracer = Optional.ofNullable(tracerPluginMap.get(tracingSystem))
            .map(tracerPlugin -> tracerPlugin.createTracer(tracerTagMap))
            .orElseThrow(() -> new ConfigException.BadValue("tracing.system",
                "No Tracing plugin found for : " + tracingSystem));

        // Register the tracer as the global tracer
        GlobalTracer.register(tracer);
        return tracer;
    }
}
