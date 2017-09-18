package net.spals.appbuilder.monitor.core;

import io.opentracing.NoopTracer;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TracerProvider}
 *
 * @author tkral
 */
public class TracerProviderTest {

    @Test
    public void testNoopDefault() {
        final TracerPlugin tracerPlugin = mock(TracerPlugin.class);
        when(tracerPlugin.createTracer(anyMap())).thenReturn(NoopTracerFactory.create());

        final TracerProvider tracerProvider = new TracerProvider(
                Collections.singletonMap("noop", tracerPlugin),
                Collections.<String, TracerTag>emptyMap()
        );

        assertThat(tracerProvider.get(), instanceOf(NoopTracer.class));
    }

    // Run after testNoopDefault because only one GlobalTracer is allowed
    @Test(dependsOnMethods = "testNoopDefault")
    public void testGlobalTracerRegistration() {
        final Tracer tracer = mock(Tracer.class);
        final TracerPlugin tracerPlugin = mock(TracerPlugin.class);
        when(tracerPlugin.createTracer(anyMap())).thenReturn(tracer);

        final TracerProvider tracerProvider = new TracerProvider(
                Collections.singletonMap("myTracer", tracerPlugin),
                Collections.<String, TracerTag>emptyMap()
        );

        tracerProvider.tracingSystem = "myTracer";
        tracerProvider.get();
        assertThat(GlobalTracer.isRegistered(), is(true));
    }
}
