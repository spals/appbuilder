package net.spals.appbuilder.executor.core;

import com.netflix.governator.annotations.Configuration;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author tkral
 */
@AutoBindSingleton(baseClass = ManagedExecutorServiceRegistry.class)
class DefaultManagedExecutorServiceRegistry implements ManagedExecutorServiceRegistry {

    @Configuration("executorService.registry.shutdown")
    private volatile Long shutdown = 1000L;

    @Configuration("executorService.registry.shutdownUnit")
    private volatile TimeUnit shutdownUnit = TimeUnit.MILLISECONDS;

    private final Set<ManagedExecutorService> managedExecutorServices = new HashSet<>();

    @Override
    public ManagedExecutorService registerExecutorService(final ExecutorService executorService) {
        final ManagedExecutorService managedExecutorService =
                new DelegatingManagedExecutorService(executorService, shutdown, shutdownUnit);

        managedExecutorServices.add(managedExecutorService);
        return managedExecutorService;
    }

    @Override
    public void stop() {
        managedExecutorServices.forEach(managedExecutorService -> {
            synchronized (managedExecutorService) {
                managedExecutorService.stop();
            }
        });
    }
}
