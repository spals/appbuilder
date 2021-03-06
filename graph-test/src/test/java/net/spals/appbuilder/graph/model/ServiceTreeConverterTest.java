package net.spals.appbuilder.graph.model;

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;
import net.spals.appbuilder.graph.model.ServiceDAGConverter.ApplicationVertex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static net.spals.appbuilder.graph.model.ServiceTreeVertex.createTreeRoot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link ServiceTreeConverter}.
 *
 * @author tkral
 */
public class ServiceTreeConverterTest {

    @DataProvider
    Object[][] basicConvertFromProvider() {
        final ApplicationVertex a = new ApplicationVertex("a");
        final IServiceDAGVertex<?> b = createDAGVertex("b");
        final IServiceDAGVertex<?> c = createDAGVertex("c");
        final IServiceDAGVertex<?> d = createDAGVertex("d");
        final IServiceDAGVertex<?> e = createDAGVertex("e");
        final IServiceDAGVertex<?> f = createDAGVertex("f");

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

    @Test(dataProvider = "basicConvertFromProvider")
    public void testBasicConvertFrom(final ServiceDAG serviceDAG,
                                     final ApplicationVertex root,
                                     final List<String> expectedBFSWalk) {
        final ServiceTree serviceTree = new ServiceTreeConverter().convertFrom(serviceDAG);

        assertThat(serviceTree.getRoot(), is(createTreeRoot(root)));
        assertThat(bfsWalk(serviceTree), is(expectedBFSWalk));
    }

    @DataProvider
    Object[][] complexConvertFromProvider() {
        final ApplicationVertex a = new ApplicationVertex("a");
        final IServiceDAGVertex<?> b = createDAGVertex("b");
        final IServiceDAGVertex<?> c = createDAGVertex("c");
        final IServiceDAGVertex<?> d = createDAGVertex("d");
        final IServiceDAGVertex<?> e = createDAGVertex("e");
        final IServiceDAGVertex<?> f = createDAGVertex("f");
        final IServiceDAGVertex<?> g = createDAGVertex("g");
        final IServiceDAGVertex<?> h = createDAGVertex("h");
        final IServiceDAGVertex<?> i = createDAGVertex("i");
        final IServiceDAGVertex<?> j = createDAGVertex("j");

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

    @Test(dataProvider = "complexConvertFromProvider")
    public void testComplexConvertFrom(final ServiceDAG serviceDAG,
                                       final IServiceDAGVertex<?> root,
                                       final List<String> expectedBFSWalk) {
        final ServiceTree serviceTree = new ServiceTreeConverter().convertFrom(serviceDAG);

        assertThat(serviceTree.getRoot(), is(createTreeRoot(root)));
        assertThat(bfsWalk(serviceTree), is(expectedBFSWalk));
    }

    private List<String> bfsWalk(final ServiceTree serviceTree) {
        final BreadthFirstIterator<IServiceTreeVertex<?>, DefaultEdge> bfs =
            new BreadthFirstIterator<>(serviceTree);

        final List<String> bfsResult = new ArrayList<>();
        while (bfs.hasNext()) {
            bfsResult.add(bfs.next().getServiceInstance().toString());
        }

        return bfsResult;
    }

    private IServiceDAGVertex<?> createDAGVertex(final String label) {
        return ServiceDAGVertex.createDAGVertex(Key.get(String.class), label);
    }

    private ServiceDAG diamondGraph(final IServiceDAGVertex<?> a,
                                    final IServiceDAGVertex<?> b,
                                    final IServiceDAGVertex<?> c,
                                    final IServiceDAGVertex<?> d) {
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
