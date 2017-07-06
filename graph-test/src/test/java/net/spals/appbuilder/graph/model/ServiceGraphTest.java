package net.spals.appbuilder.graph.model;

import com.google.inject.Key;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link ServiceGraph}
 *
 * @author tkral
 */
public class ServiceGraphTest {

    @DataProvider
    Object[][] isOrphanProvider() {
        return new Object[][] {
                // Case: Connected vertices
                {ServiceGraphVertex.newVertex(Key.get(String.class)), false},
                {ServiceGraphVertex.newVertex(Key.get(ServiceGraphTest.class)), false},
                // Case: Unconnected vertex
                {ServiceGraphVertex.newVertex(Key.get(Integer.class)), true},
                // Case: Unknown vertex
                {ServiceGraphVertex.newVertex(Key.get(Double.class)), true},
        };
    }

    @Test(dataProvider = "isOrphanProvider")
    public void testIsOrphan(final ServiceGraphVertex vertex, final boolean expectedResult) {
        final ServiceGraph serviceGraph = new ServiceGraph();

        final ServiceGraphVertex vertex1 = ServiceGraphVertex.newVertex(Key.get(String.class));
        final ServiceGraphVertex vertex2 = ServiceGraphVertex.newVertex(Key.get(ServiceGraphTest.class));
        final ServiceGraphVertex vertex3 = ServiceGraphVertex.newVertex(Key.get(Integer.class));

        serviceGraph.addVertex(vertex1).addVertex(vertex2).addVertex(vertex3).addEdge(vertex1, vertex2);
        assertThat(serviceGraph.isOrphan(vertex), is(expectedResult));
    }
}
