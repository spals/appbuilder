package net.spals.appbuilder.executor.core;

import org.inferred.freebuilder.FreeBuilder;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * A registry service for {@link ManagedExecutorService}
 * instances.
 *
 * @author tkral
 */
public interface ManagedExecutorServiceRegistry {

    /**
     * Registers the given {@link ExecutorService} to
     * this registry under the given parent class and
     * optional tags.
     */
    ManagedExecutorService registerExecutorService(Class<?> parentClass,
                                                   ExecutorService executorService,
                                                   String... tags);

    /**
     * Registers the given {@link ManagedExecutorService} to
     * this registry under the given parent class and
     * optional tags.
     */
    ManagedExecutorService registerExecutorService(Class<?> parentClass,
                                                   ManagedExecutorService managedExecutorService,
                                                   String... tags);

    /**
     * Initiate a start action on all {@link ManagedExecutorService}s
     * within this registry.
     */
    void start();

    /**
     * Initiate a start action on all {@link ManagedExecutorService}s
     * within this registry under the given parent class and
     * optional tags.
     */
    void start(Class<?> parentClass, String... tags);

    /**
     * Initiate a stop action on all {@link ManagedExecutorService}s
     * within this registry.
     */
    void stop();

    /**
     * Initiate a stop action on all {@link ManagedExecutorService}s
     * within this registry under the given parent class and
     * optional tags.
     */
    void stop(Class<?> parentClass, String... tags);

    @FreeBuilder
    interface Key {

        Class<?> getParentClass();

        Set<String> getTags();

        class Builder extends ManagedExecutorServiceRegistry_Key_Builder {  }
    }
}
