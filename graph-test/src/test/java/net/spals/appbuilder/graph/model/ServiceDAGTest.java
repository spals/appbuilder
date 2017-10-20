package net.spals.appbuilder.graph.model;

import com.google.inject.Key;
import org.testng.annotations.Test;

import java.util.Optional;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createDAGVertex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link ServiceDAG}.
 *
 * @author tkral
 */
public class ServiceDAGTest {

    @Test
    public void testDuplicateVertices() {
        final ServiceDAG serviceDAG = new ServiceDAG();

        final IServiceDAGVertex<?> vertex1 = createDAGVertex(Key.get(String.class), "vertex");
        final IServiceDAGVertex<?> vertex2 = createDAGVertex(Key.get(String.class), "vertex");

        serviceDAG.addVertex(vertex1);
        serviceDAG.addVertex(vertex2);

        assertThat(serviceDAG.vertexSet(), hasSize(1));
    }

    @Test
    public void testFindVertex() {
        final ServiceDAG serviceDAG = new ServiceDAG();

        final IServiceDAGVertex<?> vertex = createDAGVertex(Key.get(String.class), "vertex");
        serviceDAG.addVertex(vertex);

        final Optional<IServiceDAGVertex<?>> findVertex = serviceDAG.findVertex(Key.get(String.class));
        assertThat(findVertex, not(Optional.empty()));
        assertThat(findVertex.get(), is(vertex));
    }

    @Test
    public void testFindAllVertices() {
        final ServiceDAG serviceDAG = new ServiceDAG();

        final IServiceDAGVertex<?> vertex = createDAGVertex(Key.get(String.class), "vertex");
        serviceDAG.addVertex(vertex);

        assertThat(serviceDAG.findAllVertices(rawTypeThat(subclassesOf(String.class))), contains(vertex));
    }
}
