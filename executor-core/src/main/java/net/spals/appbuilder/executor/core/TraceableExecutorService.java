package net.spals.appbuilder.executor.core;

import com.google.common.annotations.VisibleForTesting;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * An {@link ExecutorService} delegater which provides
 * asynchronous tracing a {@link Tracer} object.
 *
 * @author tkral
 */
class TraceableExecutorService implements ExecutorService {

    private final ExecutorService delegate;
    private final TracedExecutorService tracedDelegate;

    TraceableExecutorService(final ExecutorService delegate,
                             final Tracer tracer) {
        this.delegate = delegate;
        this.tracedDelegate = new TracedExecutorService(delegate, tracer);
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return tracedDelegate.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        tracedDelegate.execute(command);
    }

    @VisibleForTesting
    ExecutorService getDelegate() {
        return delegate;
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return tracedDelegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
                                         final long timeout,
                                         final TimeUnit unit) throws InterruptedException {
        return tracedDelegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return tracedDelegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks,
                           final long timeout,
                           final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return tracedDelegate.invokeAny(tasks, timeout, unit);
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
    public void shutdown() {
        tracedDelegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return tracedDelegate.shutdownNow();
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return tracedDelegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return tracedDelegate.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return tracedDelegate.submit(task);
    }
}
