package net.spals.appbuilder.app.core;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.typesafe.config.Config;
import net.spals.appbuilder.graph.writer.ServiceGraphWriter;
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
        // 1. Startup the Governator LifecycleManager
        final LifecycleManager lifecycleManager = getLifecycleInjector().getLifecycleManager();
        lifecycleManager.start();
        // 2. Ensure that we shut everything down properly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            getLogger().info("Shutting down {} application.", getName());
            lifecycleManager.close();
        }));
        // 3. Grab the Guice injector from which we can get service references
        final Injector serviceInjecter = getLifecycleInjector().createInjector();
        // 4. Output the service graph
        final ServiceGraphWriter serviceGraphWriter = serviceInjecter.getInstance(Key.get(ServiceGraphWriter.class));
        serviceGraphWriter.writeServiceGraph();

        return serviceInjecter;
    }
}
