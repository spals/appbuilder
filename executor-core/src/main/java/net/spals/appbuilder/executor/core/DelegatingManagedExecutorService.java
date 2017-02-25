package net.spals.appbuilder.executor.core;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author tkral
 */
class DelegatingManagedExecutorService implements ManagedExecutorService {

    private final ExecutorService executorServiceDelegate;

    private final Logger logger;

    private final long shutdown;
    private final TimeUnit shutdownUnit;

    DelegatingManagedExecutorService(final ExecutorService executorServiceDelegate,
                                     final ManagedExecutorServiceRegistry.Key executorServiceKey,
                                     final long shutdown,
                                     final TimeUnit shutdownUnit) {
        this.executorServiceDelegate = executorServiceDelegate;

        final String loggerName = String.format("%s[%s]", executorServiceKey.getParentClass().getName(),
                Joiner.on(',').join(executorServiceKey.getTags()));
        this.logger = LoggerFactory.getLogger(loggerName);

        this.shutdown = shutdown;
        this.shutdownUnit = shutdownUnit;
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return executorServiceDelegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorServiceDelegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
                                         final long timeout,
                                         final TimeUnit unit) throws InterruptedException {
        return executorServiceDelegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorServiceDelegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks,
                           final long timeout,
                           final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorServiceDelegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public boolean isShutdown() {
        return executorServiceDelegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorServiceDelegate.isTerminated();
    }

    @Override
    public void execute(final Runnable command) {
        executorServiceDelegate.execute(command);
    }

    @Override
    public void shutdown() {
        executorServiceDelegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorServiceDelegate.shutdownNow();
    }

    @Override
    public synchronized void stop() {
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
        return executorServiceDelegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return executorServiceDelegate.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return executorServiceDelegate.submit(task);
    }
}
