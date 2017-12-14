package net.spals.appbuilder.executor.core;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
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

    private Cache<Key, ExecutorService> executorServices;
    private final Tracer tracer;

    @Configuration("executorService.registry.shutdown")
    private volatile Long shutdown = 1000L;
    @Configuration("executorService.registry.shutdownUnit")
    private volatile TimeUnit shutdownUnit = TimeUnit.MILLISECONDS;

    @Inject
    DefaultExecutorServiceFactory(final Tracer tracer) {
        this.tracer = tracer;
    }

    @PostConstruct
    @VisibleForTesting
    void setupCache() {
        this.executorServices = CacheBuilder.<Key, ExecutorService>newBuilder()
            .removalListener(new ExecutorServiceRemovelListener(shutdown, shutdownUnit))
            .build();
    }

    @Override
    public ExecutorService createCachedThreadPool(@Nonnull final Key key) {
        final ExecutorService delegate = Executors.newCachedThreadPool();

        final ExecutorService executorService = decorateExecutorService(delegate);
        executorServices.put(key, executorService);
        getExecutorServiceLogger(key).info("Created CachedThreadPool executor service");

        return executorService;
    }

    @Override
    public ExecutorService createFixedThreadPool(
        final int nThreads,
        @Nonnull final Key key
    ) {
        final ExecutorService delegate = Executors.newFixedThreadPool(nThreads);

        final ExecutorService executorService = decorateExecutorService(delegate);
        executorServices.put(key, executorService);
        getExecutorServiceLogger(key).info("Created FixThreadPool executor service");

        return executorService;
    }

    @Override
    public ExecutorService createSingleThreadExecutor(@Nonnull final Key key) {
        final ExecutorService delegate = Executors.newSingleThreadExecutor();

        final ExecutorService executorService = decorateExecutorService(delegate);
        executorServices.put(key, executorService);
        getExecutorServiceLogger(key).info("Created SingleThreadExecutor executor service");

        return executorService;
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
    public ScheduledExecutorService createSingleThreadScheduledExecutor(@Nonnull final Key key) {
        final ScheduledExecutorService delegate = Executors.newSingleThreadScheduledExecutor();

        final ScheduledExecutorService scheduledExecutorService = decorateScheduledExecutorService(delegate);
        executorServices.put(key, scheduledExecutorService);
        getExecutorServiceLogger(key).info("Created SingleThreadScheduledExecutor scheduled executor service");

        return scheduledExecutorService;
    }

    @Override
    public Optional<ExecutorService> get(@Nonnull final Key key) {
        return Optional.ofNullable(executorServices.getIfPresent(key));
    }

    @Override
    public void stop(@Nonnull final Key key) {
        get(key).ifPresent(executorService -> executorServices.invalidate(key));
    }

    private ExecutorService decorateExecutorService(final ExecutorService delegate) {
        // Wrap the delegate as a traced executor service so we get nice asynchronous tracing
        return new TracedExecutorService(delegate, tracer);
    }

    private ScheduledExecutorService decorateScheduledExecutorService(final ScheduledExecutorService delegate) {
        // Wrap the delegate as a traced scheduled executor service so we get nice asynchronous tracing
        return new TracedScheduledExecutorService(delegate, tracer);
    }

    private static Logger getExecutorServiceLogger(final Key key) {
        final String loggerName = key.getParentClass().getName() + key.getTags().stream().collect(JOINING);
        return LoggerFactory.getLogger(loggerName);
    }

    @VisibleForTesting
    Map<Key, ExecutorService> getExecutorServices() {
        return executorServices.asMap();
    }

    /**
     * Stops all executors for this factory before the factory is destroyed by the overall service.
     */
    @PreDestroy
    public synchronized void stopAll() {
        executorServices.invalidateAll();
    }


    private static class ExecutorServiceRemovelListener implements RemovalListener<Key, ExecutorService> {

        private final long shutdown;
        private final TimeUnit shutdownUnit;

        private ExecutorServiceRemovelListener(
            final long shutdown,
            final TimeUnit shutdownUnit
        ) {
            this.shutdown = shutdown;
            this.shutdownUnit = shutdownUnit;
        }

        @Override
        public void onRemoval(final RemovalNotification<Key, ExecutorService> notification) {
            final Key key = notification.getKey();
            final ExecutorService executorService = notification.getValue();

            if (executorService.isShutdown()) {
                return;
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
        }
    }
}
