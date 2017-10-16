package net.spals.appbuilder.graph.model;

import com.google.inject.Key;

/**
 * Interface definition of a vertex
 * which appears in a {@link ServiceGraph}
 * instance.
 *
 * @author tkral
 */
public interface IServiceGraphVertex<T> {

    Key<T> getGuiceKey();
    T getServiceInstance();

    String toString(String separator);
}