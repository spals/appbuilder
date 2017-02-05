package net.spals.appbuilder.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Guice Module to be auto-bound
 * in a set within an application.
 *
 * Guice Modules will be bound during the bootstrap
 * phase of an application, which allows them to
 * dynamically bind services at boot time.
 *
 * @author tkral
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoBindModule {  }
