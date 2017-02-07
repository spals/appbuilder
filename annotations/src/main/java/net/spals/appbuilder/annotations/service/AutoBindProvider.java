package net.spals.appbuilder.annotations.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static net.spals.appbuilder.annotations.service.AutoBindProvider.ProviderScope.SINGLETON;

/**
 * Marks a service provider to be auto-bound
 * within an application.
 *
 * See https://github.com/google/guice/wiki/ProviderBindings
 *
 * @author tkral
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoBindProvider {

    ProviderScope value() default SINGLETON;

    enum ProviderScope {
        NONE,
        REQUEST,
        SESSION,
        SINGLETON,
        ;
    }
}
