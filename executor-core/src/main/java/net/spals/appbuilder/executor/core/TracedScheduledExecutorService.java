package net.spals.appbuilder.executor.core;

import com.google.common.annotations.VisibleForTesting;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.contrib.concurrent.TracedRunnable;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

/**
 * An {@link ExecutorService} delegator which provides asynchronous tracing a {@link Tracer} object.
 *
 * @author spags
 */
public class TracedScheduledExecutorService extends TracedExecutorService implements ScheduledExecutorService {

    private final ScheduledExecutorService delegate;
    private final Tracer tracer;

    TracedScheduledExecutorService(
        final ScheduledExecutorService delegate,
        final Tracer tracer
    ) {
        super(delegate, tracer);
        this.delegate = delegate;
        this.tracer = tracer;
    }

    @Nonnull
    @Override
    public <V> ScheduledFuture<V> schedule(
        @Nonnull final Callable<V> callable,
        final long delay,
        @Nonnull final TimeUnit unit
    ) {
        return delegate.schedule(
            new TracedCallable<>(callable, tracer.activeSpan()),
            delay,
            unit
        );
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> schedule(
        @Nonnull final Runnable command,
        final long delay,
        @Nonnull final TimeUnit unit
    ) {
        return delegate.schedule(
            new TracedRunnable(command, tracer.activeSpan()),
            delay,
            unit
        );
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
        @Nonnull final Runnable command,
        final long initialDelay,
        final long period,
        @Nonnull final TimeUnit unit
    ) {
        return delegate.scheduleAtFixedRate(
            new TracedRunnable(command, tracer.activeSpan()),
            initialDelay,
            period,
            unit
        );
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
        @Nonnull final Runnable command,
        final long initialDelay,
        final long delay,
        @Nonnull final TimeUnit unit
    ) {
        return delegate.scheduleWithFixedDelay(
            new TracedRunnable(command, tracer.activeSpan()),
            initialDelay,
            delay,
            unit
        );
    }

    @VisibleForTesting
    ScheduledExecutorService getDelegate() {
        return delegate;
    }
}