package net.spals.appbuilder.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a singleton service to be auto-bound
 * as a key-value pair in a map within an application.
 *
 * See https://github.com/google/guice/wiki/Multibindings
 *
 * @author tkral
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoBindInMap {

    /**
     * The type under which the service should be bound. This
     * effectively acts as the value type within the map binder.
     *
     * Under almost all circumstances, this should be an interface.
     */
    Class<?> baseClass();

    /**
     * The key value that maps to the service within the map binder.
     */
    String key();

    /**
     * The type of the key that maps to the service within the map binder.
     *
     * Note that this can only be a String or Enum.
     */
    Class<?> keyType() default String.class;
}
