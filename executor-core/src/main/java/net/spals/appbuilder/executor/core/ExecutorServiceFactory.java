package net.spals.appbuilder.executor.core;

import org.inferred.freebuilder.FreeBuilder;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author tkral
 */
public interface ExecutorServiceFactory {

    ExecutorService createFixedThreadPool(int nThreads,
                                          Class<?> parentClass,
                                          String... nameSuffixes);

    ExecutorService createCachedThreadPool(Class<?> parentClass,
                                           String... nameSuffixes);

    ExecutorService createSingleThreadExecutor(Class<?> parentClass,
                                               String... nameSuffixes);

    @FreeBuilder
    interface Key {

        Class<?> getParentClass();

        Set<String> getTags();

        class Builder extends ExecutorServiceFactory_Key_Builder {  }
    }
}
