package net.spals.appbuilder.executor.core;

import java.util.concurrent.ExecutorService;

/**
 * @author tkral
 */
public interface ManagedExecutorServiceRegistry {

    ManagedExecutorService registerExecutorService(ExecutorService executorService);

    default void start() {
        // Normally nothing to do. ExecutorServices are ready to take on jobs once created.
    }

    void stop();
}
