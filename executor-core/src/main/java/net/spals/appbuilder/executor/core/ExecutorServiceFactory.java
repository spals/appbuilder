package net.spals.appbuilder.executor.core;

import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author tkral
 */
public interface ExecutorServiceFactory {

    ExecutorService createFixedThreadPool(int nThreads, Key key);

    ExecutorService createCachedThreadPool(Key key);

    ExecutorService createSingleThreadExecutor(Key key);

    ScheduledExecutorService createSingleThreadScheduledExecutor(Key key);

    ScheduledExecutorService createScheduledThreadPool(int nThreads, Key key);

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

            public Builder(final Class<?> parentClass) {
                setParentClass(parentClass);
            }
        }
    }
}
