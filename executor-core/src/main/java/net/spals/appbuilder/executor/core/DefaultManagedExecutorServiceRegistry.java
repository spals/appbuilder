package net.spals.appbuilder.executor.core;

import com.netflix.governator.annotations.Configuration;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link ManagedExecutorServiceRegistry}.
 *
 * @author tkral
 */
@AutoBindSingleton(baseClass = ManagedExecutorServiceRegistry.class)
class DefaultManagedExecutorServiceRegistry implements ManagedExecutorServiceRegistry {

    @Configuration("executorService.registry.shutdown")
    private volatile Long shutdown = 1000L;

    @Configuration("executorService.registry.shutdownUnit")
    private volatile TimeUnit shutdownUnit = TimeUnit.MILLISECONDS;

    private final Map<Key, ManagedExecutorService> managedExecutorServices = new HashMap<>();


    @Override
    public ManagedExecutorService registerExecutorService(final Class<?> parentClass,
                                                          final ExecutorService executorService,
                                                          final String... tags) {
        final Key key = new Key.Builder().setParentClass(parentClass).addTags(tags).build();
        final ManagedExecutorService managedExecutorService =
                new DelegatingManagedExecutorService(executorService, shutdown, shutdownUnit);

        return managedExecutorServices.put(key, managedExecutorService);
    }

    @Override
    public ManagedExecutorService registerExecutorService(final Class<?> parentClass,
                                                          final ManagedExecutorService managedExecutorService,
                                                          final String... tags) {
        final Key key = new Key.Builder().setParentClass(parentClass).addTags(tags).build();
        return managedExecutorServices.put(key, managedExecutorService);
    }

    @Override
    public synchronized void start() {
        managedExecutorServices.values()
                .forEach(managedExecutorService -> managedExecutorService.start());
    }

    @Override
    public synchronized void start(final Class<?> parentClass, final String... tags) {
        final Key key = new Key.Builder().setParentClass(parentClass).addTags(tags).build();
        Optional.ofNullable(managedExecutorServices.get(key))
                .ifPresent(managedExecutorService -> managedExecutorService.start());
    }

    @Override
    public synchronized void stop() {
        managedExecutorServices.values()
                .forEach(managedExecutorService -> managedExecutorService.stop());
    }

    @Override
    public synchronized void stop(final Class<?> parentClass, final String... tags) {
        final Key key = new Key.Builder().setParentClass(parentClass).addTags(tags).build();
        Optional.ofNullable(managedExecutorServices.get(key))
                .ifPresent(managedExecutorService -> managedExecutorService.stop());
    }
}
