package net.spals.appbuilder.executor.core;

import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedRunnable;
import io.opentracing.mock.MockTracer;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TraceableExecutorService}.
 *
 * @author tkral
 */
public class TraceableExecutorServiceTest {

    @Test
    public void testExecute() throws InterruptedException {
        final ExecutorService delegate = mock(ExecutorService.class);

        final MockTracer tracer = new MockTracer();
        final TraceableExecutorService traceableExecutorService = new TraceableExecutorService(delegate, tracer);

        final Runnable r = () -> {};
        traceableExecutorService.execute(r);

        verify(delegate).execute(isA(TracedRunnable.class));
    }

    @Test
    public void testSubmitCallable() {
        final ExecutorService delegate = mock(ExecutorService.class);

        final MockTracer tracer = new MockTracer();
        final TraceableExecutorService traceableExecutorService = new TraceableExecutorService(delegate, tracer);

        final Callable<Void> c = () -> (Void)null;
        traceableExecutorService.submit(c);
        verify(delegate).submit(isA(TracedCallable.class));
    }

    @Test
    public void testSubmitRunnable() {
        final ExecutorService delegate = mock(ExecutorService.class);

        final MockTracer tracer = new MockTracer();
        final TraceableExecutorService traceableExecutorService = new TraceableExecutorService(delegate, tracer);

        final Runnable r = () -> {};
        traceableExecutorService.submit(r);
        verify(delegate).submit(isA(TracedRunnable.class));
    }

    @Test
    public void testSubmitRunnableResult() {
        final ExecutorService delegate = mock(ExecutorService.class);

        final MockTracer tracer = new MockTracer();
        final TraceableExecutorService traceableExecutorService = new TraceableExecutorService(delegate, tracer);

        final Runnable r = () -> {};
        traceableExecutorService.submit(r, 1L);
        verify(delegate).submit(isA(TracedRunnable.class), eq(1L));
    }
}
