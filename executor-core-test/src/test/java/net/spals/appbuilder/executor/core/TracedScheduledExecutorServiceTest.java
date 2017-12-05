package net.spals.appbuilder.executor.core;

import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedRunnable;
import io.opentracing.mock.MockTracer;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TracedScheduledExecutorService}.
 *
 * @author tkral
 */
public class TracedScheduledExecutorServiceTest {

    @Test
    public void testScheduleCallable() throws InterruptedException {
        final ScheduledExecutorService delegate = mock(ScheduledExecutorService.class);
        // Satisfy @Nonnull annotation
        doReturn(mock(ScheduledFuture.class)).when(delegate)
            .schedule(any(Callable.class), anyLong(), any(TimeUnit.class));

        final MockTracer tracer = new MockTracer();
        final TracedScheduledExecutorService tracedScheduledExecutorService =
            new TracedScheduledExecutorService(delegate, tracer);

        final Callable<Void> c = () -> (Void)null;
        tracedScheduledExecutorService.schedule(c, 1L, TimeUnit.SECONDS);

        verify(delegate).schedule(isA(TracedCallable.class), eq(1L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testScheduleRunnable() {
        final ScheduledExecutorService delegate = mock(ScheduledExecutorService.class);
        // Satisfy @Nonnull annotation
        doReturn(mock(ScheduledFuture.class)).when(delegate)
            .schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        final MockTracer tracer = new MockTracer();
        final TracedScheduledExecutorService tracedScheduledExecutorService =
            new TracedScheduledExecutorService(delegate, tracer);

        final Runnable r = () -> {};
        tracedScheduledExecutorService.schedule(r, 1L, TimeUnit.SECONDS);

        verify(delegate).schedule(isA(TracedRunnable.class), eq(1L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testScheduleAtFixedRate() {
        final ScheduledExecutorService delegate = mock(ScheduledExecutorService.class);
        // Satisfy @Nonnull annotation
        doReturn(mock(ScheduledFuture.class)).when(delegate)
            .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        final MockTracer tracer = new MockTracer();
        final TracedScheduledExecutorService tracedScheduledExecutorService =
            new TracedScheduledExecutorService(delegate, tracer);

        final Runnable r = () -> {};
        tracedScheduledExecutorService.scheduleAtFixedRate(r, 1L, 1L, TimeUnit.SECONDS);

        verify(delegate).scheduleAtFixedRate(isA(TracedRunnable.class), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testScheduleWithFixedDelay() {
        final ScheduledExecutorService delegate = mock(ScheduledExecutorService.class);
        // Satisfy @Nonnull annotation
        doReturn(mock(ScheduledFuture.class)).when(delegate)
            .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        final MockTracer tracer = new MockTracer();
        final TracedScheduledExecutorService tracedScheduledExecutorService =
            new TracedScheduledExecutorService(delegate, tracer);

        final Runnable r = () -> {};
        tracedScheduledExecutorService.scheduleWithFixedDelay(r, 1L, 1L, TimeUnit.SECONDS);

        verify(delegate).scheduleWithFixedDelay(isA(TracedRunnable.class), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
    }
}
