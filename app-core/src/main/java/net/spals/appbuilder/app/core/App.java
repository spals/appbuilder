package net.spals.appbuilder.app.core;

import com.google.inject.Injector;
import com.typesafe.config.Config;
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
