package net.spals.appbuilder.app.core;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.typesafe.config.Config;
import org.slf4j.Logger;

/**
 * @author tkral
 */
public interface App {

    Logger getLogger();

    LifecycleInjector getLifecycleInjector();

    String getName();

    Config getServiceConfig();

    default Injector getServiceInjector() throws Exception {
        final LifecycleManager lifecycleManager = getLifecycleInjector().getLifecycleManager();
        lifecycleManager.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            getLogger().info("Shutting down {} application.", getName());
            lifecycleManager.close();
        }));
        return getLifecycleInjector().createInjector();
    }
}
