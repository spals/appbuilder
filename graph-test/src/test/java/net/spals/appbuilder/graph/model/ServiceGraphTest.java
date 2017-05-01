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
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build(), false},
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(ServiceGraphTest.class)).build(), false},
                // Case: Unconnected vertex
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(Integer.class)).build(), true},
                // Case: Unknown vertex
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(Double.class)).build(), true},
        };
    }

    @Test(dataProvider = "isOrphanProvider")
    public void testIsOrphan(final ServiceGraphVertex vertex, final boolean expectedResult) {
        final ServiceGraph serviceGraph = new ServiceGraph();

        final ServiceGraphVertex vertex1 = new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build();
        final ServiceGraphVertex vertex2 = new ServiceGraphVertex.Builder().setGuiceKey(Key.get(ServiceGraphTest.class)).build();
        final ServiceGraphVertex vertex3 = new ServiceGraphVertex.Builder().setGuiceKey(Key.get(Integer.class)).build();

        serviceGraph.addVertex(vertex1).addVertex(vertex2).addVertex(vertex3).addEdge(vertex1, vertex2);
        assertThat(serviceGraph.isOrphan(vertex), is(expectedResult));
    }
}
