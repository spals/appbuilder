package net.spals.appbuilder.graph.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.inject.Key;

import java.util.Optional;

import static net.spals.appbuilder.graph.model.ServiceGraphVertex.DEFAULT_SEPARATOR;
import static net.spals.appbuilder.graph.model.ServiceGraphVertex.createGraphVertex;

/**
 * @author tkral
 */
@AutoValue
public abstract class ServiceDAGVertex<T> implements IServiceDAGVertex<T> {

    abstract IServiceGraphVertex<T> getDelegate();

    public static <T2> ServiceDAGVertex<T2> createDAGVertex(final IServiceGraphVertex<T2> vertex) {
        return new AutoValue_ServiceDAGVertex<>(vertex, Optional.empty());
    }

    public static <T2> ServiceDAGVertex<T2> createDAGVertex(final Key<T2> guiceKey, final T2 serviceInstance) {
        return new AutoValue_ServiceDAGVertex<>(createGraphVertex(guiceKey, serviceInstance), Optional.empty());
    }

    static <T2> ServiceDAGVertex<T2> createDAGVertexWithProvider(final IServiceGraphVertex<T2> vertex,
                                                                 final IServiceDAGVertex<?> providerSource) {
        Preconditions.checkNotNull(providerSource);
        return new AutoValue_ServiceDAGVertex<>(vertex, Optional.of(providerSource));
    }

    static <T2> ServiceDAGVertex<T2> createDAGVertexWithProvider(final Key<T2> guiceKey,
                                                                 final T2 serviceInstance,
                                                                 final IServiceDAGVertex<?> providerSource) {
        Preconditions.checkNotNull(providerSource);
        return new AutoValue_ServiceDAGVertex<>(createGraphVertex(guiceKey, serviceInstance),
            Optional.of(providerSource));
    }

    @Override
    public final Key<T> getGuiceKey() {
        return getDelegate().getGuiceKey();
    }

    @Override
    public final T getServiceInstance() {
        return getDelegate().getServiceInstance();
    }

    @Override
    public abstract Optional<IServiceDAGVertex<?>> getProviderSource();

    @Override
    public final String toString() {
        return toString(DEFAULT_SEPARATOR);
    }

    @Override
    public String toString(final String separator) {
        final StringBuilder sb = new StringBuilder(getDelegate().toString(separator));
        if (getProviderSource().isPresent()) {
            sb.append(separator).append("[Provider:");
            sb.append(ServiceGraphVertex.typeLiteralName(getProviderSource().get().getGuiceKey().getTypeLiteral()));
            sb.append("]");
        }

        return sb.toString();
    }
}
