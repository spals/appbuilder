package net.spals.appbuilder.executor.core;

import java.util.concurrent.ExecutorService;

/**
 * @author tkral
 */
public interface ManagedExecutorService extends ExecutorService {

    void stop();
}
