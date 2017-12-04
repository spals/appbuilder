package net.spals.appbuilder.executor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class StoppableScheduledExecutorService implements ScheduledExecutorService {

    private static final Collector<CharSequence, ?, String> JOINING = Collectors.joining(",", "[", "]");
    private final ScheduledExecutorService delegate;
    private final Logger logger;
    private final long shutdown;
    private final TimeUnit shutdownUnit;

    StoppableScheduledExecutorService(
        final ScheduledExecutorService delegate,
        final ExecutorServiceFactory.Key key,
        final long shutdown,
        final TimeUnit shutdownUnit
    ) {
        this.delegate = delegate;
        final String loggerName = key.getParentClass().getName()
            + key.getTags().stream().collect(JOINING);
        this.logger = LoggerFactory.getLogger(loggerName);
        this.shutdown = shutdown;
        this.shutdownUnit = shutdownUnit;
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> schedule(
        @Nonnull final Runnable command,
        final long delay,
        @Nonnull final TimeUnit unit
    ) {
        return delegate.schedule(command, delay, unit);
    }

    @Nonnull
    @Override
    public <V> ScheduledFuture<V> schedule(
        @Nonnull final Callable<V> callable,
        final long delay,
        @Nonnull final TimeUnit unit
    ) {
        return delegate.schedule(callable, delay, unit);
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
        @Nonnull final Runnable command,
        final long initialDelay,
        final long period,
        @Nonnull final TimeUnit unit
    ) {
        return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Nonnull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
        @Nonnull final Runnable command,
        final long initialDelay,
        final long delay,
        @Nonnull final TimeUnit unit
    ) {
        return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Nonnull
    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull final Callable<T> task) {
        return delegate.submit(task);
    }

    @Nonnull
    @Override
    public <T> Future<T> submit(@Nonnull final Runnable task, final T result) {
        return delegate.submit(task, result);
    }

    @Nonnull
    @Override
    public Future<?> submit(@Nonnull final Runnable task) {
        return delegate.submit(task);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(
        @Nonnull final Collection<? extends Callable<T>> tasks
    ) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Nonnull
    @Override
    public <T> List<Future<T>> invokeAll(
        @Nonnull final Collection<? extends Callable<T>> tasks,
        final long timeout,
        @Nonnull final TimeUnit unit
    ) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Nonnull
    @Override
    public <T> T invokeAny(
        @Nonnull final Collection<? extends Callable<T>> tasks
    ) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(
        @Nonnull final Collection<? extends Callable<T>> tasks,
        final long timeout,
        @Nonnull final TimeUnit unit
    ) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@Nonnull final Runnable command) {
        delegate.execute(command);
    }

    synchronized void stop() {
        if (isShutdown()) {
            return;
        }

        // See https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
        shutdown(); // Disable new tasks from being submitted

        try {
            // Wait a while for existing tasks to terminate
            if (!awaitTermination(shutdown, shutdownUnit)) {
                shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!awaitTermination(shutdown, shutdownUnit)) {
                    logger.warn("Timed out waiting for ExecutorService to terminate.");
                }
            }
        } catch (final InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
            logger.warn("Interrupted during shutdown of ExecutorService.");
        }
    }
}
