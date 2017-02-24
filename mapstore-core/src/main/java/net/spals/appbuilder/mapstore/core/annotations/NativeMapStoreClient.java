package net.spals.appbuilder.mapstore.core.annotations;

import com.google.inject.BindingAnnotation;
import net.spals.appbuilder.mapstore.core.MapStore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the native client of a {@link MapStore}
 * implementation.
 *
 * @author tkral
 */
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface NativeMapStoreClient {
}
