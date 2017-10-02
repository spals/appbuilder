package net.spals.appbuilder.graph.model;

import com.google.auto.value.AutoValue;
import com.google.common.base.Objects;
import com.google.inject.Key;
import com.google.inject.grapher.Node;

import java.util.Optional;

/**
 * @author tkral
 */
@AutoValue
public abstract class ServiceGraphVertex<T> {

    public static <T2> ServiceGraphVertex<T2> newVertex(final Key<T2> guiceKey, final T2 serviceInstance) {
        return new AutoValue_ServiceGraphVertex(guiceKey, serviceInstance);
    }

    public abstract Key<T> getGuiceKey();
    public abstract T getServiceInstance();

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(ServiceGraphVertex.class.isAssignableFrom(obj.getClass()))) {
            return false;
        }
        final ServiceGraphVertex that = (ServiceGraphVertex) obj;
        return Objects.equal(getGuiceKey(), that.getGuiceKey()) && Objects.equal(getServiceInstance(), that.getServiceInstance());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getGuiceKey(), getServiceInstance());
    }
}
