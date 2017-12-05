package net.spals.appbuilder.executor.core;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.inferred.freebuilder.FreeBuilder;

/**
 * @author tkral
 */
public interface ExecutorServiceFactory {

    ExecutorService createFixedThreadPool(
        int nThreads,
        Key key
    );

    default ExecutorService createFixedThreadPool(
        final int nThreads,
        final Class<?> parentClass,
        final String... tags
    ) {
        return createFixedThreadPool(
            nThreads,
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    ExecutorService createCachedThreadPool(Key key);

    default ExecutorService createCachedThreadPool(
        final Class<?> parentClass,
        final String... tags
    ) {
        return createCachedThreadPool(
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    ExecutorService createSingleThreadExecutor(Key key);

    default ExecutorService createSingleThreadExecutor(
        final Class<?> parentClass,
        final String... tags
    ) {
        return createSingleThreadExecutor(
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    ScheduledExecutorService createSingleThreadScheduledExecutor(Key key);

    default ScheduledExecutorService createSingleThreadScheduledExecutor(
        final Class<?> parentClass,
        final String... tags
    ) {
        return createSingleThreadScheduledExecutor(
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    ScheduledExecutorService createScheduledThreadPool(
        int nThreads,
        Key key
    );

    default ScheduledExecutorService createScheduledThreadPool(
        final int nThreads,
        final Class<?> parentClass,
        final String... tags
    ) {
        return createScheduledThreadPool(
            nThreads,
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    /**
     * Stops the executor of the given key. If the key exists, then shutdown the executor returning an optional of true
     * if the shutdown was successful and fail if it was not.
     * If the key does NOT exist, then no-ops and returns empty.
     * <p>
     * Performs stop via the recommend oracle best practice <a href='https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html'>here</a>
     * </p>
     *
     * @param key key of the executor
     * @return true if the key exists and the executor is now stopped, false if it exists but stop logic failed, otherwise empty
     */
    Optional<Boolean> stop(Key key);

    /**
     * Gets the executor of the given key if it exists. If an executor does NOT exist, then returns {@link Optional#empty()}.
     *
     * @param key key of the executor
     * @return an executor if it exists otherwise empty
     */
    Optional<ExecutorService> get(Key key);

    @FreeBuilder
    interface Key {

        Class<?> getParentClass();

        Set<String> getTags();

        class Builder extends ExecutorServiceFactory_Key_Builder {

        }
    }
}
