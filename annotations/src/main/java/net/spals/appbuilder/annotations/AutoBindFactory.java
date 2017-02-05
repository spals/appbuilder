package net.spals.appbuilder.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service factory to be auto-bound
 * within an application.
 *
 * Factories are created via Guice's AssistedInject
 * feature.
 *
 * See https://github.com/google/guice/wiki/AssistedInject
 *
 * @author tkral
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoBindFactory {  }
