package net.spals.appbuilder.graph.model;

import com.google.auto.value.AutoValue;
import com.google.inject.Key;

/**
 * A {@link ServiceDAGVertex} wrapper which
 * allows a caller to customize the separator
 * string used in serializing the vertex.
 *
 * @author tkral
 */
@AutoValue
public abstract class PrintableVertex<T> implements IServiceGraphVertex<T> {

    public static <T2> PrintableVertex<T2> createPrintableVertex(final IServiceGraphVertex<T2> delegate,
                                                                 final String separator) {
        return new AutoValue_PrintableVertex<>(delegate, separator);
    }

    abstract IServiceGraphVertex<T> getDelegate();
    abstract String getSeparator();

    @Override
    public Key<T> getGuiceKey() {
        return getDelegate().getGuiceKey();
    }

    @Override
    public T getServiceInstance() {
        return getDelegate().getServiceInstance();
    }

    @Override
    public final String toString() {
        return toString(getSeparator());
    }

    @Override
    public String toString(final String separator) {
        return getDelegate().toString(separator);
    }
}