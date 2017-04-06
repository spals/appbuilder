package net.spals.appbuilder.graph.model;

import com.google.common.base.Objects;
import com.google.inject.Key;
import com.google.inject.grapher.Node;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class ServiceGraphVertex {

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

    public static class Builder extends ServiceGraphVertex_Builder {
        public Builder setNode(final Node guiceNode) {
            setGuiceKey(guiceNode.getId().getKey());
            return setSource(Optional.ofNullable(guiceNode.getSource()).map(source -> source.getClass()));
        }
    }
}
