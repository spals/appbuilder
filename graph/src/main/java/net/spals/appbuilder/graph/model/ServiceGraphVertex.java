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
public abstract class ServiceGraphVertex {

    public static ServiceGraphVertex newVertex(final Key<?> guiceKey) {
        return new AutoValue_ServiceGraphVertex(guiceKey, Optional.empty());
    }

    public static ServiceGraphVertex newVertex(final Key<?> guiceKey, final Class<?> source) {
        return new AutoValue_ServiceGraphVertex(guiceKey, Optional.of(source));
    }

    public static ServiceGraphVertex newVertex(final Node guiceNode) {
        return new AutoValue_ServiceGraphVertex(guiceNode.getId().getKey(),
                Optional.ofNullable(guiceNode.getSource()).map(source -> source.getClass()));
    }

    public abstract Key<?> getGuiceKey();
    public abstract Optional<Class<?>> getSource();

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(ServiceGraphVertex.class.isAssignableFrom(obj.getClass()))) {
            return false;
        }
        final ServiceGraphVertex that = (ServiceGraphVertex) obj;
        return Objects.equal(getGuiceKey(), that.getGuiceKey()) && Objects.equal(getSource(), that.getSource());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getGuiceKey(), getSource());
    }
}
