package net.spals.appbuilder.annotations.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a singleton service to be auto-bound
 * in a set within an application.
 *
 * See https://github.com/google/guice/wiki/Multibindings
 *
 * @author tkral
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoBindInSet {

    /**
     * The type under which the service should be bound. This
     * effectively acts as the generic type within the set binder.
     *
     * Under almost all circumstances, this should be an interface.
     */
    Class<?> baseClass();
}
