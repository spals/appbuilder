package net.spals.appbuilder.executor.core;

import org.inferred.freebuilder.FreeBuilder;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author tkral
 */
public interface ExecutorServiceFactory {

    ExecutorService createFixedThreadPool(
        int nThreads,
        Key key
    );

    default ExecutorService createFixedThreadPool(
        int nThreads,
        Class<?> parentClass,
        String... tags
    ) {
        return createFixedThreadPool(
            nThreads,
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    ExecutorService createCachedThreadPool(Key key);

    default ExecutorService createCachedThreadPool(
        Class<?> parentClass,
        String... tags
    ) {
        return createCachedThreadPool(
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    ExecutorService createSingleThreadExecutor(Key key);

    default ExecutorService createSingleThreadExecutor(
        Class<?> parentClass,
        String... tags
    ) {
        return createSingleThreadExecutor(
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    ScheduledExecutorService createSingleThreadScheduledExecutor(Key key);

    default ScheduledExecutorService createSingleThreadScheduledExecutor(
        Class<?> parentClass,
        String... tags
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
        int nThreads,
        Class<?> parentClass,
        String... tags
    ) {
        return createScheduledThreadPool(
            nThreads,
            new Key.Builder().setParentClass(parentClass).addTags(tags).build()
        );
    }

    @FreeBuilder
    interface Key {

        Class<?> getParentClass();

        Set<String> getTags();

        class Builder extends ExecutorServiceFactory_Key_Builder {
        }
    }
}
