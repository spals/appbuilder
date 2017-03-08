package net.spals.appbuilder.app.core.grapher;

import com.google.common.base.Objects;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.grapher.Node;
import org.inferred.freebuilder.FreeBuilder;

import java.io.IOException;
import java.util.Optional;

/**
 * @author tkral
 */
public interface ServiceGrapher {

    default ServiceGrapher addEdge(final Key<?> fromKey, final Key<?> toKey) {
        return addEdge(new Vertex.Builder().setGuiceKey(fromKey).build(),
                new Vertex.Builder().setGuiceKey(toKey).build());
    }

    ServiceGrapher addEdge(Vertex fromVertex, Vertex toVertex);

    default ServiceGrapher addVertex(final Key<?> guiceKey) {
        return addVertex(new Vertex.Builder().setGuiceKey(guiceKey).build());
    }

    ServiceGrapher addVertex(Vertex vertex);

    Type getType();

    void graph(Injector injector) throws IOException;

    @FreeBuilder
    abstract class Vertex {

        public abstract Key<?> getGuiceKey();
        public abstract Optional<Class<?>> getSource();

        @Override
        public boolean equals(final Object obj) {
            if (obj == null || !(Vertex.class.isAssignableFrom(obj.getClass()))) {
                return false;
            }
            final Vertex that = (Vertex) obj;
            return Objects.equal(getGuiceKey(), that.getGuiceKey()) && Objects.equal(getSource(), that.getSource());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getGuiceKey(), getSource());
        }

        public static class Builder extends ServiceGrapher_Vertex_Builder {
            public Builder setNode(final Node guiceNode) {
                setGuiceKey(guiceNode.getId().getKey());
                return setSource(Optional.ofNullable(guiceNode.getSource()).map(source -> source.getClass()));
            }
        }
    }

    enum Type {
        NO_OP,
        ASCII,
//        GRAPHVIZ,
        ;
    }
}
