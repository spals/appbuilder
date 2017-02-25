package net.spals.appbuilder.executor.core;

import java.util.concurrent.ExecutorService;

/**
 * An {@link ExecutorService} which can be externally
 * managed via start and stop hooks.
 *
 * @author tkral
 */
public interface ManagedExecutorService extends ExecutorService {

    default void start() {
        // Normally nothing to do. ExecutorServices are ready to take on jobs once created.
    }

    void stop();
}
