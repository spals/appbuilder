package net.spals.appbuilder.executor.core;

import com.google.inject.Inject;
import com.google.common.annotations.VisibleForTesting;
import com.netflix.governator.annotations.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link ExecutorServiceFactory}.
 *
 * @author tkral
 */
@AutoBindSingleton(baseClass = ExecutorServiceFactory.class)
class DefaultExecutorServiceFactory implements ExecutorServiceFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExecutorServiceFactory.class);

    @Configuration("executorService.registry.shutdown")
    private volatile Long shutdown = 1000L;

    @Configuration("executorService.registry.shutdownUnit")
    private volatile TimeUnit shutdownUnit = TimeUnit.MILLISECONDS;

    private final Map<Key, StoppableExecutorService> stoppableExecutorServices = new HashMap<>();
    private final Tracer tracer;

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

        final StoppableExecutorService stoppableExecutorService = decorateExecutorService(delegate, key);
        logExecutorService("FixedThreadPool", key);
        stoppableExecutorServices.put(key, stoppableExecutorService);
        return stoppableExecutorService;
    }

    @Override
    public ExecutorService createCachedThreadPool(final Key key) {
        final ExecutorService delegate = Executors.newCachedThreadPool();

        final StoppableExecutorService stoppableExecutorService = decorateExecutorService(delegate, key);
        logExecutorService("CachedThreadPool", key);
        stoppableExecutorServices.put(key, stoppableExecutorService);
        return stoppableExecutorService;
    }

    @Override
    public ExecutorService createSingleThreadExecutor(final Key key) {
        final ExecutorService delegate = Executors.newSingleThreadExecutor();

        final StoppableExecutorService stoppableExecutorService = decorateExecutorService(delegate, key);
        logExecutorService("SingleThreadExecutor", key);
        stoppableExecutorServices.put(key, stoppableExecutorService);
        return stoppableExecutorService;
    }

    @Override
    public ExecutorService createSingleThreadScheduledExecutor(final Key key) {
        final ExecutorService delegate = Executors.newSingleThreadScheduledExecutor();

        final StoppableExecutorService stoppableExecutorService = decorateExecutorService(delegate, key);
        logExecutorService("SingleThreadScheduledExecutor", key);
        stoppableExecutorServices.put(key, stoppableExecutorService);
        return stoppableExecutorService;
    }

    StoppableExecutorService decorateExecutorService(
        final ExecutorService delegate,
        final Key key
    ) {
        // First, wrap the delegate as a traced executor service so we get nice asynchronous tracing
        final ExecutorService traceableExecutorService = new TraceableExecutorService(delegate, tracer);
        // Second, wrap the delegate as a stoppable executor service so we can gracefully shutdown
        // at the end of the application's life
        final StoppableExecutorService stoppableExecutorService =
            new StoppableExecutorService(traceableExecutorService, key, shutdown, shutdownUnit);

        return stoppableExecutorService;
    }

    void logExecutorService(final String description, final Key key) {
        LOGGER.info("Created " + description + " executor service for " + key.getParentClass().getSimpleName() +
            "(" + key.getTags() + ")");
    }

    @VisibleForTesting
    Map<Key, StoppableExecutorService> getStoppableExecutorServices() {
        return stoppableExecutorServices;
    }

    @PreDestroy
    public synchronized void stop() {
        stoppableExecutorServices.entrySet()
            .forEach(entry -> {
                LOGGER.info("Shutting down ExecutorService for " +
                    entry.getKey().getParentClass().getSimpleName() + "(" + entry.getKey().getTags() + ")");
                entry.getValue().stop();
            });
    }
}
