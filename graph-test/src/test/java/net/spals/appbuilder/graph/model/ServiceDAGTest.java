package net.spals.appbuilder.graph.model;

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createVertex;
import static net.spals.appbuilder.graph.model.ServiceTreeVertex.createRoot;
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

        final ServiceDAGVertex<?> vertex1 = createDAGVertex("vertex");
        final ServiceDAGVertex<?> vertex2 = createDAGVertex("vertex");

        serviceDAG.addVertex(vertex1);
        serviceDAG.addVertex(vertex2);

        assertThat(serviceDAG.vertexSet(), hasSize(1));
    }

    @Test
    public void testFindVertex() {
        final ServiceDAG serviceDAG = new ServiceDAG();

        final ServiceDAGVertex<?> vertex = createDAGVertex("vertex");
        serviceDAG.addVertex(vertex);

        final Optional<ServiceDAGVertex<?>> findVertex = serviceDAG.findVertex(Key.get(String.class));
        assertThat(findVertex, not(Optional.empty()));
        assertThat(findVertex.get(), is(vertex));
    }

    @Test
    public void testFindAllVertices() {
        final ServiceDAG serviceDAG = new ServiceDAG();

        final ServiceDAGVertex<?> vertex = createDAGVertex("vertex");
        serviceDAG.addVertex(vertex);

        assertThat(serviceDAG.findAllVertices(rawTypeThat(subclassesOf(String.class))), contains(vertex));
    }

    @DataProvider
    Object[][] basicToTreeProvider() {
        final ServiceDAGVertex<?> a = createDAGVertex("a");
        final ServiceDAGVertex<?> b = createDAGVertex("b");
        final ServiceDAGVertex<?> c = createDAGVertex("c");
        final ServiceDAGVertex<?> d = createDAGVertex("d");
        final ServiceDAGVertex<?> e = createDAGVertex("e");
        final ServiceDAGVertex<?> f = createDAGVertex("f");

        final ServiceDAG basicDiamond = diamondGraph(a, b, c, d);

        final ServiceDAG basicY = diamondGraph(a, b, c, d);
        basicY.addVertex(e);
        basicY.addEdge(e, d);

        final ServiceDAG basicX = diamondGraph(a, b, c, d);
        basicX.addVertex(e);
        basicX.addEdge(e, d);
        basicX.addVertex(f);
        basicX.addEdge(f, d);

        final ServiceDAG longY = diamondGraph(a, b, c, d);
        longY.addVertex(e);
        longY.addEdge(e, d);
        longY.addVertex(f);
        longY.addEdge(f, e);

        return new Object[][] {
            {basicDiamond, a, ImmutableList.of("a", "b", "c", "d", "d")},
            {basicY, a, ImmutableList.of("a", "b", "c", "d", "d", "e", "e")},
            {basicX, a, ImmutableList.of("a", "b", "c", "d", "d", "e", "f", "e", "f")},
            {longY, a, ImmutableList.of("a", "b", "c", "d", "d", "e", "e", "f", "f")},
        };
    }

    @Test(dataProvider = "basicToTreeProvider")
    public void testBasicToTree(final ServiceDAG serviceDAG,
                                final ServiceDAGVertex<?> root,
                                final List<String> expectedBFSWalk) {
        final ServiceTree serviceTree = serviceDAG.toTree(root);

        assertThat(serviceTree.getRoot(), is(createRoot(root)));
        assertThat(bfsWalk(serviceTree), is(expectedBFSWalk));
    }

    @DataProvider
    Object[][] complexToTreeProvider() {
        final ServiceDAGVertex<?> a = createDAGVertex("a");
        final ServiceDAGVertex<?> b = createDAGVertex("b");
        final ServiceDAGVertex<?> c = createDAGVertex("c");
        final ServiceDAGVertex<?> d = createDAGVertex("d");
        final ServiceDAGVertex<?> e = createDAGVertex("e");
        final ServiceDAGVertex<?> f = createDAGVertex("f");
        final ServiceDAGVertex<?> g = createDAGVertex("g");
        final ServiceDAGVertex<?> h = createDAGVertex("h");
        final ServiceDAGVertex<?> i = createDAGVertex("i");
        final ServiceDAGVertex<?> j = createDAGVertex("j");

        final ServiceDAG multiDiamond = diamondGraph(a, c, d, f);
        multiDiamond.addVertex(b);
        multiDiamond.addEdge(b, a);
        multiDiamond.addVertex(e);
        multiDiamond.addEdge(e, a);
        multiDiamond.addVertex(g);
        multiDiamond.addEdge(g, b);
        multiDiamond.addVertex(h);
        multiDiamond.addEdge(h, f);
        multiDiamond.addVertex(i);
        multiDiamond.addEdge(i, f);
        multiDiamond.addVertex(j);
        multiDiamond.addEdge(j, e);

        multiDiamond.addEdge(h, b);
        multiDiamond.addEdge(i, e);

        final ServiceDAG largeDiamond = new ServiceDAG();
        largeDiamond.addVertex(a);
        largeDiamond.addVertex(b);
        largeDiamond.addVertex(c);
        largeDiamond.addEdge(c, b);
        largeDiamond.addEdge(b, a);

        largeDiamond.addVertex(d);
        largeDiamond.addVertex(e);
        largeDiamond.addVertex(f);
        largeDiamond.addEdge(b, f);
        largeDiamond.addEdge(f, e);
        largeDiamond.addEdge(e, d);
        largeDiamond.addEdge(d, a);

        return new Object[][] {
            {multiDiamond, a, ImmutableList.of("a", "c", "d", "b", "e", "f", "f", "g", "h", "j", "i", "h", "i", "h", "i")},
            {largeDiamond, a, ImmutableList.of("a", "b", "d", "c", "e", "f", "b", "c")},
        };
    }

    @Test(dataProvider = "complexToTreeProvider")
    public void testComplexToTree(final ServiceDAG serviceDAG,
                                  final ServiceDAGVertex<?> root,
                                  final List<String> expectedBFSWalk) {
        final ServiceTree serviceTree = serviceDAG.toTree(root);

        assertThat(serviceTree.getRoot(), is(createRoot(root)));
        assertThat(bfsWalk(serviceTree), is(expectedBFSWalk));
    }

    private List<String> bfsWalk(final ServiceTree serviceTree) {
        final BreadthFirstIterator<ServiceTreeVertex<?>, DefaultEdge> bfs =
            new BreadthFirstIterator<>(serviceTree);

        final List<String> bfsResult = new ArrayList<>();
        while (bfs.hasNext()) {
            bfsResult.add(bfs.next().getDelegate().getServiceInstance().toString());
        }

        return bfsResult;
    }

    private ServiceDAGVertex<?> createDAGVertex(final String label) {
        return createVertex(Key.get(String.class), label);
    }

    private ServiceDAG diamondGraph(final ServiceDAGVertex<?> a,
                                    final ServiceDAGVertex<?> b,
                                    final ServiceDAGVertex<?> c,
                                    final ServiceDAGVertex<?> d) {
        final ServiceDAG serviceDAG = new ServiceDAG();

        serviceDAG.addVertex(a);
        serviceDAG.addVertex(b);
        serviceDAG.addVertex(c);
        serviceDAG.addVertex(d);

        // Create a DAG in a "diamond" shape
        serviceDAG.addEdge(b, a);
        serviceDAG.addEdge(c, a);
        serviceDAG.addEdge(d, b);
        serviceDAG.addEdge(d, c);

        return serviceDAG;
    }
}
