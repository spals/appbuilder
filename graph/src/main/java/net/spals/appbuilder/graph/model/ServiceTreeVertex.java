package net.spals.appbuilder.graph.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.inject.Key;

import java.util.Optional;

/**
 * @author tkral
 */
@AutoValue
public abstract class ServiceTreeVertex<T> implements IServiceTreeVertex<T> {

    abstract IServiceDAGVertex<T> getDelegate();
    public abstract Optional<IServiceTreeVertex<?>> getParent();

    static <T2> ServiceTreeVertex<T2> createTreeChild(final IServiceDAGVertex<T2> delegate,
                                                      final IServiceTreeVertex<?> parent) {
        Preconditions.checkNotNull(parent);
        return new AutoValue_ServiceTreeVertex<>(delegate, Optional.of(parent));
    }

    static <T2> ServiceTreeVertex<T2> createTreeRoot(final IServiceDAGVertex<T2> delegate) {
        return new AutoValue_ServiceTreeVertex<>(delegate, Optional.empty());
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
    public final Optional<IServiceDAGVertex<?>> getProviderSource() {
        return getDelegate().getProviderSource();
    }

    @Override
    public final boolean equals(final Object obj) {
        if (!getDelegate().equals(obj) || !(IServiceTreeVertex.class.isAssignableFrom(obj.getClass()))) {
            return false;
        }

        final IServiceTreeVertex that = (IServiceTreeVertex) obj;
        return Objects.equal(getParent(), that.getParent());
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(getDelegate().hashCode(), getParent());
    }

    @Override
    public final String toString() {
        return getDelegate().toString();
    }

    @Override
    public final String toString(final String separator) {
        return getDelegate().toString(separator);
    }
}
