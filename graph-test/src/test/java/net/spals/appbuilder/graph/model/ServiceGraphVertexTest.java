package net.spals.appbuilder.graph.model;

import com.google.inject.Key;
import com.google.inject.Provider;
import net.spals.appbuilder.annotations.config.ApplicationName;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link ServiceGraphVertex}.
 *
 * @author tkral
 */
public class ServiceGraphVertexTest {

    @DataProvider
    Object[][] equalsProvider() {
        return new Object[][] {
                // Case: Non-Vertex objects
                {ServiceGraphVertex.newVertex(Key.get(String.class)), null, false},
                {ServiceGraphVertex.newVertex(Key.get(String.class)), new Object(), false},
                // Case: Same key types
                {ServiceGraphVertex.newVertex(Key.get(String.class)),
                        ServiceGraphVertex.newVertex(Key.get(String.class)), true},
                // Case: Different key types
                {ServiceGraphVertex.newVertex(Key.get(String.class)),
                        ServiceGraphVertex.newVertex(Key.get(Integer.class)), false},
                // Case: Mismatched annotations
                {ServiceGraphVertex.newVertex(Key.get(String.class, ApplicationName.class)),
                        ServiceGraphVertex.newVertex(Key.get(String.class)), false},
                // Case: Mismatched sources
                {ServiceGraphVertex.newVertex(Key.get(String.class), Provider.class),
                        ServiceGraphVertex.newVertex(Key.get(String.class)), false},
                // Case: Same key types, same annotations, same sources
                {ServiceGraphVertex.newVertex(Key.get(String.class, ApplicationName.class), Provider.class),
                        ServiceGraphVertex.newVertex(Key.get(String.class, ApplicationName.class), Provider.class), true},
        };
    }

    @Test(dataProvider = "equalsProvider")
    public void testEquals(final ServiceGraphVertex vertex, final Object obj, final boolean expectedResult) {
        assertThat(vertex.equals(obj), is(expectedResult));
    }

    @Test(dataProvider = "equalsProvider")
    public void testHashCode(final ServiceGraphVertex vertex, final Object obj, final boolean expectedResult) {
        if (obj != null && obj instanceof ServiceGraphVertex) {
            final Matcher<Integer> hashCodeMatcher = expectedResult ? is(vertex.hashCode()) : not(vertex.hashCode());
            assertThat(obj.hashCode(), hashCodeMatcher);
        }
    }

    @Test
    public void testInHashSet() {
        final HashSet<ServiceGraphVertex> vertexSet = new HashSet<>();
        vertexSet.add(ServiceGraphVertex.newVertex(Key.get(String.class)));
        vertexSet.add(ServiceGraphVertex.newVertex(Key.get(String.class)));

        assertThat(vertexSet, hasSize(1));
    }
}
