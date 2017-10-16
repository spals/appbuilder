package net.spals.appbuilder.graph.model;

import java.util.Optional;

/**
 * Interface definition of a vertex
 * which appears in a {@link ServiceTree}
 * instance.
 *
 * @author tkral
 */
public interface IServiceTreeVertex<T> extends IServiceDAGVertex<T> {

    Optional<IServiceTreeVertex<?>> getParent();

    default boolean isRoot() {
        return !getParent().isPresent();
    }
}