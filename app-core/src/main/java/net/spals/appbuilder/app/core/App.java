package net.spals.appbuilder.app.core;

import com.google.inject.Injector;
import com.typesafe.config.Config;
import org.slf4j.Logger;

/**
 * Contract definition for an application built
 * by an appbuilder.
 *
 * This contract represents the most basic
 * components of a running application which
 * serves as a template for implementing classes.
 *
 * Implementing are not limited to this contract.
 * They may provide further state to consumers
 * as appropriate.
 *
 * @author tkral
 */
public interface App {

    /**
     * Return the main logger for the application.
     *
     * Typically, this is the logger associated
     * with the application's main method.
     */
    Logger getLogger();

    /**
     * Return the name of the application.
     */
    String getName();

    /**
     * Return all service configuration
     * that is to be used within the application.
     */
    Config getServiceConfig();

    /**
     * Return the Guice {@link Injector}
     * which holds state for all of the
     * application's services.
     */
    Injector getServiceInjector();
}
