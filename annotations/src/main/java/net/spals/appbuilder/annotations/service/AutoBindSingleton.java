package net.spals.appbuilder.annotations.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a singleton service to be auto-bound
 * within an application.
 *
 * @author tkral
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoBindSingleton {

    /**
     * The type under which the service should be bound.
     *
     * By default, the service will be bound to the implementing class.
     */
    Class<?> baseClass() default Void.class;

    /**
     * Indicates that the service should be bound under
     * both its implementing class and an alternate base class
     * (i.e. an implemented interface).
     */
    boolean includeImpl() default false;
}
