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

    String getName();

    Config getServiceConfig();

    Injector getServiceInjector();
}
