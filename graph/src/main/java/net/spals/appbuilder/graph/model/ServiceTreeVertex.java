package net.spals.appbuilder.graph.model;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.inject.Key;

import java.util.Optional;

/**
 * @author tkral
 */
public class ServiceTreeVertex<T> extends ServiceDAGVertex<T> {

    private final ServiceDAGVertex<T> delegate;
    private final Optional<ServiceTreeVertex<?>> parent;

    static <T2> ServiceTreeVertex<T2> createChild(final ServiceDAGVertex<T2> delegate,
                                                  final ServiceTreeVertex<?> parent) {
        Preconditions.checkNotNull(parent);
        return new ServiceTreeVertex<T2>(delegate, Optional.of(parent));
    }

    static <T2> ServiceTreeVertex<T2> createRoot(final ServiceDAGVertex<T2> delegate) {
        return new ServiceTreeVertex<T2>(delegate, Optional.empty());
    }

    private ServiceTreeVertex(final ServiceDAGVertex<T> delegate,
                              final Optional<ServiceTreeVertex<?>> parent) {
        this.delegate = delegate;
        this.parent = parent;
    }

    ServiceDAGVertex<?> getDelegate() {
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

    public Optional<ServiceTreeVertex<?>> getParent() {
        return parent;
    }

    @Override
    public Optional<ServiceDAGVertex<?>> getProviderSource() {
        return delegate.getProviderSource();
    }

    public boolean isRoot() {
        return !parent.isPresent();
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
    protected String toString(final String separator) {
        return delegate.toString(separator);
    }
}
