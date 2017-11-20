package net.spals.appbuilder.executor.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * An {@link ExecutorService} delegater which provides
 * a {@link #stop()} method to run a graceful shutdown
 * of the executor.
 *
 * @author tkral
 */
class StoppableExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    private final Logger logger;

    private final long shutdown;
    private final TimeUnit shutdownUnit;

    StoppableExecutorService(
        final ExecutorService delegate,
        final ExecutorServiceFactory.Key key,
        final long shutdown,
        final TimeUnit shutdownUnit
    ) {
        this.delegate = delegate;

        final String loggerName = String.format("%s[%s]", key.getParentClass().getName(),
            Joiner.on(',').join(key.getTags()));
        this.logger = LoggerFactory.getLogger(loggerName);

        this.shutdown = shutdown;
        this.shutdownUnit = shutdownUnit;
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        delegate.execute(command);
    }

    @VisibleForTesting
    ExecutorService getDelegate() {
        return delegate;
    }

    @VisibleForTesting
    Logger getLogger() {
        return logger;
    }

    @VisibleForTesting
    long getShutdown() {
        return shutdown;
    }

    @VisibleForTesting
    TimeUnit getShutdownUnit() {
        return shutdownUnit;
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(
        final Collection<? extends Callable<T>> tasks,
        final long timeout,
        final TimeUnit unit
    ) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(
        final Collection<? extends Callable<T>> tasks,
        final long timeout,
        final TimeUnit unit
    ) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
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
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
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
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
            logger.warn("Interrupted during shutdown of ExecutorService.");
        }
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return delegate.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return delegate.submit(task);
    }
}
