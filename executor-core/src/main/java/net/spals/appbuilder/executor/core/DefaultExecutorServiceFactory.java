package net.spals.appbuilder.executor.core;

import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.netflix.governator.annotations.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

/**
 * Default implementation of {@link ExecutorServiceFactory}.
 *
 * @author tkral
 */
@AutoBindSingleton(baseClass = ExecutorServiceFactory.class)
class DefaultExecutorServiceFactory implements ExecutorServiceFactory {

    private static final Collector<CharSequence, ?, String> JOINING = Collectors.joining(",", "[", "]");
    private final Map<Key, ExecutorService> executorServices = new HashMap<>();
    private final Tracer tracer;
    @Configuration("executorService.registry.shutdown")
    private volatile Long shutdown = 1000L;
    @Configuration("executorService.registry.shutdownUnit")
    private volatile TimeUnit shutdownUnit = TimeUnit.MILLISECONDS;

    @Inject
    DefaultExecutorServiceFactory(final Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public ExecutorService createFixedThreadPool(
        final int nThreads,
        final Key key
    ) {
        final ExecutorService delegate = Executors.newFixedThreadPool(nThreads);

        final ExecutorService executorService = decorateExecutorService(delegate);
        executorServices.put(key, executorService);
        getExecutorServiceLogger(key).info("Created FixThreadPool executor service");

        return executorService;
    }

    @Override
    public ExecutorService createCachedThreadPool(final Key key) {
        final ExecutorService delegate = Executors.newCachedThreadPool();

        final ExecutorService executorService = decorateExecutorService(delegate);
        executorServices.put(key, executorService);
        getExecutorServiceLogger(key).info("Created CachedThreadPool executor service");

        return executorService;
    }

    @Override
    public ExecutorService createSingleThreadExecutor(final Key key) {
        final ExecutorService delegate = Executors.newSingleThreadExecutor();

        final ExecutorService executorService = decorateExecutorService(delegate);
        executorServices.put(key, executorService);
        getExecutorServiceLogger(key).info("Created SingleThreadExecutor executor service");

        return executorService;
    }

    @Override
    public ScheduledExecutorService createSingleThreadScheduledExecutor(final Key key) {
        final ScheduledExecutorService delegate = Executors.newSingleThreadScheduledExecutor();

        final ScheduledExecutorService scheduledExecutorService = decorateScheduledExecutorService(delegate);
        executorServices.put(key, scheduledExecutorService);
        getExecutorServiceLogger(key).info("Created SingleThreadScheduledExecutor scheduled executor service");

        return scheduledExecutorService;
    }

    @Override
    public ScheduledExecutorService createScheduledThreadPool(
        final int nThreads,
        final Key key
    ) {
        final ScheduledExecutorService delegate = Executors.newScheduledThreadPool(nThreads);

        final ScheduledExecutorService scheduledExecutorService = decorateScheduledExecutorService(delegate);
        executorServices.put(key, scheduledExecutorService);
        getExecutorServiceLogger(key).info("Created ScheduledThreadPool scheduled executor service");

        return scheduledExecutorService;
    }

    @Override
    public Optional<Boolean> stop(@Nonnull final Key key) {
        return get(key).map(x -> stopExecutorService(key, x));
    }

    @Override
    public Optional<ExecutorService> get(@Nonnull final Key key) {
        return Optional.ofNullable(executorServices.get(key));
    }

    private ExecutorService decorateExecutorService(final ExecutorService delegate) {
        // Wrap the delegate as a traced executor service so we get nice asynchronous tracing
        return new TracedExecutorService(delegate, tracer);
    }

    private ScheduledExecutorService decorateScheduledExecutorService(final ScheduledExecutorService delegate) {
        // Wrap the delegate as a traced scheduled executor service so we get nice asynchronous tracing
        return new TracedScheduledExecutorService(delegate, tracer);
    }

    private Logger getExecutorServiceLogger(final Key key) {
        final String loggerName = key.getParentClass().getName() + key.getTags().stream().collect(JOINING);
        return LoggerFactory.getLogger(loggerName);
    }

    @VisibleForTesting
    Map<Key, ExecutorService> getExecutorServices() {
        return executorServices;
    }

    /**
     * Stops all executors for this factory before the factory is destroyed by the overall service.
     */
    @PreDestroy
    public synchronized void stopAll() {
        // TODO TPK: Is it OK that all executor service shutdowns happen sequentially?
        executorServices.forEach(this::stopExecutorService);
    }

    @VisibleForTesting
    boolean stopExecutorService(
        @Nonnull final Key key,
        @Nonnull final ExecutorService executorService
    ) {
        if (executorService.isShutdown()) {
            return false;
        }

        final Logger executorServiceLogger = getExecutorServiceLogger(key);
        executorServiceLogger.info(
            "Shutting down ExecutorService for " + key.getParentClass().getSimpleName() +
                "(" + key.getTags() + ")"
        );
        // See https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
        executorService.shutdown(); // Disable new tasks from being submitted

        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(shutdown, shutdownUnit)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(shutdown, shutdownUnit)) {
                    executorServiceLogger.warn("Timed out waiting for ExecutorService to terminate.");
                }
            }
        } catch (final InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
            executorServiceLogger.warn("Interrupted during shutdown of ExecutorService.");
        }
        return true;
    }
}
