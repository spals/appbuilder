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
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build(), null, false},
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build(), new Object(), false},
                // Case: Same key types
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build(),
                        new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build(), true},
                // Case: Different key types
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build(),
                        new ServiceGraphVertex.Builder().setGuiceKey(Key.get(Integer.class)).build(), false},
                // Case: Mismatched annotations
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class, ApplicationName.class)).build(),
                        new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build(), false},
                // Case: Mismatched sources
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).setSource(Provider.class).build(),
                        new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build(), false},
                // Case: Same key types, same annotations, same sources
                {new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class, ApplicationName.class)).setSource(Provider.class).build(),
                        new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class, ApplicationName.class)).setSource(Provider.class).build(), true},
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
        vertexSet.add(new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build());
        vertexSet.add(new ServiceGraphVertex.Builder().setGuiceKey(Key.get(String.class)).build());

        assertThat(vertexSet, hasSize(1));
    }
}
