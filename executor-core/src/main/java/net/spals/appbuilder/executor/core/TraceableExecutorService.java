package net.spals.appbuilder.executor.core;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * An {@link ExecutorService} delegator which provides
 * asynchronous tracing a {@link Tracer} object.
 *
 * @author tkral
 */
class TraceableExecutorService implements ExecutorService {

    private final ExecutorService delegate;
    private final TracedExecutorService tracedDelegate;

    TraceableExecutorService(
        final ExecutorService delegate,
        final Tracer tracer
    ) {
        this.delegate = delegate;
        this.tracedDelegate = new TracedExecutorService(delegate, tracer);
    }

    @Override
    public void shutdown() {
        tracedDelegate.shutdown();
    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        return tracedDelegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return tracedDelegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return tracedDelegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException {
        return tracedDelegate.awaitTermination(timeout, unit);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull final Callable<T> task) {
        return tracedDelegate.submit(task);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull final Runnable task, final T result) {
        return tracedDelegate.submit(task, result);
    }

    @Nonnull
    @Override
    public Future<?> submit(@Nonnull final Runnable task) {
        return tracedDelegate.submit(task);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(
        @Nonnull final Collection<? extends Callable<T>> tasks
    ) throws InterruptedException {
        return tracedDelegate.invokeAll(tasks);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(
        @Nonnull final Collection<? extends Callable<T>> tasks,
        final long timeout,
        @Nonnull final TimeUnit unit
    ) throws InterruptedException {
        return tracedDelegate.invokeAll(tasks, timeout, unit);
    }

    @Nonnull
    @Override
    public <T> T invokeAny(
        @Nonnull final Collection<? extends Callable<T>> tasks
    ) throws InterruptedException, ExecutionException {
        return tracedDelegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(
        @Nonnull final Collection<? extends Callable<T>> tasks,
        final long timeout,
        @Nonnull final TimeUnit unit
    ) throws InterruptedException, ExecutionException, TimeoutException {
        return tracedDelegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@Nonnull final Runnable command) {
        tracedDelegate.execute(command);
    }

    @VisibleForTesting
    ExecutorService getDelegate() {
        return delegate;
    }
}
