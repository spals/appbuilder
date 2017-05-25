package net.spals.appbuilder.executor.core;

import com.netflix.governator.annotations.Configuration;
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

    private final Map<Key, DelegatingManagedExecutorService> managedExecutorServices = new HashMap<>();

    @Override
    public ExecutorService createFixedThreadPool(final int nThreads,
                                                 final Class<?> parentClass,
                                                 final String... tags) {
        final Key key = new Key.Builder().setParentClass(parentClass).addTags(tags).build();
        final DelegatingManagedExecutorService managedExecutorService =
                new DelegatingManagedExecutorService(Executors.newFixedThreadPool(nThreads), key, shutdown, shutdownUnit);

        LOGGER.info("Created FixedThreadPool executor service for " + key.getParentClass().getSimpleName() +
                "(" + key.getTags() + ")");
        managedExecutorServices.put(key, managedExecutorService);
        return managedExecutorService;
    }

    @Override
    public ExecutorService createCachedThreadPool(final Class<?> parentClass,
                                                  final String... tags) {
        final Key key = new Key.Builder().setParentClass(parentClass).addTags(tags).build();
        final DelegatingManagedExecutorService managedExecutorService =
                new DelegatingManagedExecutorService(Executors.newCachedThreadPool(), key, shutdown, shutdownUnit);

        LOGGER.info("Created CachedThreadPool executor service for " + key.getParentClass().getSimpleName() +
                "(" + key.getTags() + ")");
        managedExecutorServices.put(key, managedExecutorService);
        return managedExecutorService;
    }

    @Override
    public ExecutorService createSingleThreadExecutor(final Class<?> parentClass,
                                                      final String... tags) {
        final Key key = new Key.Builder().setParentClass(parentClass).addTags(tags).build();
        final DelegatingManagedExecutorService managedExecutorService =
                new DelegatingManagedExecutorService(Executors.newSingleThreadExecutor(), key, shutdown, shutdownUnit);

        LOGGER.info("Created SingleThreadExecutor executor service for " + key.getParentClass().getSimpleName() +
                "(" + key.getTags() + ")");
        managedExecutorServices.put(key, managedExecutorService);
        return managedExecutorService;
    }

    @PreDestroy
    public synchronized void stop() {
        managedExecutorServices.entrySet()
                .forEach(entry -> {
                    LOGGER.info("Shutting down ExecutorService for " +
                            entry.getKey().getParentClass().getSimpleName() + "(" + entry.getKey().getTags() + ")");
                    entry.getValue().stop();
                });
    }
}
