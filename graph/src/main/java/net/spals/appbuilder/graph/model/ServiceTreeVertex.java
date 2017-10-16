package net.spals.appbuilder.graph.model;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.inject.Key;

import java.util.Optional;

/**
 * @author tkral
 */
public class ServiceTreeVertex<T> implements IServiceTreeVertex<T> {

    private final IServiceDAGVertex<T> delegate;
    private final Optional<IServiceTreeVertex<?>> parent;

    static <T2> ServiceTreeVertex<T2> createChild(final IServiceDAGVertex<T2> delegate,
                                                  final IServiceTreeVertex<?> parent) {
        Preconditions.checkNotNull(parent);
        return new ServiceTreeVertex<T2>(delegate, Optional.of(parent));
    }

    static <T2> ServiceTreeVertex<T2> createRoot(final IServiceDAGVertex<T2> delegate) {
        return new ServiceTreeVertex<T2>(delegate, Optional.empty());
    }

    private ServiceTreeVertex(final IServiceDAGVertex<T> delegate,
                              final Optional<IServiceTreeVertex<?>> parent) {
        this.delegate = delegate;
        this.parent = parent;
    }

    IServiceDAGVertex<T> getDelegate() {
        return delegate;
    }

    @Override
    public Key<T> getGuiceKey() {
        return delegate.getGuiceKey();
    }

    @Override
    public T getServiceInstance() {
        return delegate.getServiceInstance();
    }

    @Override
    public Optional<IServiceTreeVertex<?>> getParent() {
        return parent;
    }

    @Override
    public Optional<IServiceDAGVertex<?>> getProviderSource() {
        return delegate.getProviderSource();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(ServiceTreeVertex.class.isAssignableFrom(obj.getClass()))) {
            return false;
        }
        final ServiceTreeVertex that = (ServiceTreeVertex) obj;
        return Objects.equal(getGuiceKey(), that.getGuiceKey()) &&
            Objects.equal(getServiceInstance(), that.getServiceInstance()) &&
            Objects.equal(getParent(), that.getParent()) &&
            Objects.equal(getProviderSource(), that.getProviderSource());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getGuiceKey(), getServiceInstance(), getParent(), getProviderSource());
    }

    @Override
    public String toString(final String separator) {
        return delegate.toString(separator);
    }
}
