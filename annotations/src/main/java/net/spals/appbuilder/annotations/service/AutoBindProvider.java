package net.spals.appbuilder.annotations.service;

import java.lang.annotation.*;

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

    Class<? extends Annotation> bindingAnnotation() default AutoBindProvider.class;

    enum ProviderScope {
        LAZY_SINGLETON,
        NONE,
        REQUEST,
        SESSION,
        SINGLETON,
        ;
    }
}
