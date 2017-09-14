package net.spals.appbuilder.monitor.core;

import io.opentracing.Tracer;

/**
 * A plugin definition for creating a
 * {@link Tracer} object specific to a
 * particular opentracing implementation.
 *
 * @author tkral
 */
public interface TracerPlugin {

    Tracer createTracer();
}
