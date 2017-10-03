package net.spals.appbuilder.graph.model;

import com.google.inject.Key;

import java.util.Optional;

/**
 * A {@link ServiceGraphVertex} wrapper which
 * allows a caller to customize the separator
 * string used in serializing the vertex.
 *
 * @author tkral
 */
public class PrintableServiceVertex<T> extends ServiceGraphVertex<T> {

    private final ServiceGraphVertex<T> delegate;
    private final String separator;

    public PrintableServiceVertex(final ServiceGraphVertex<T> delegate, final String separator) {
        this.delegate = delegate;
        this.separator = separator;
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
    public Optional<ServiceGraphVertex<?>> getProviderSource() {
        return delegate.getProviderSource();
    }

    @Override
    public String toString() {
        return delegate.toString(separator);
    }
}
