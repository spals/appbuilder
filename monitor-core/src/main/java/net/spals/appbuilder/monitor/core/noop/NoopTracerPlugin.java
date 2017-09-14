package net.spals.appbuilder.monitor.core.noop;

import io.opentracing.NoopTracer;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.monitor.core.TracerPlugin;

/**
 * A {@link TracerPlugin} for the
 * {@link NoopTracer}.
 *
 * @author tkral
 */
@AutoBindInMap(baseClass = TracerPlugin.class, key = "noop")
class NoopTracerPlugin implements TracerPlugin {

    @Override
    public Tracer createTracer() {
        return NoopTracerFactory.create();
    }
}
