
package net.spals.appbuilder.graph.model;

import java.util.Optional;

/**
 * Interface definition of a vertex
 * which appears in a {@link ServiceDAG}
 * instance.
 *
 * @author tkral
 */
public interface IServiceDAGVertex<T> extends IServiceGraphVertex<T> {

    Optional<IServiceDAGVertex<?>> getProviderSource();
}