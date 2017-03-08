package net.spals.appbuilder.app.core.grapher;

import com.google.inject.Key;
import com.google.inject.Provider;
import net.spals.appbuilder.annotations.config.ApplicationName;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.core.grapher.ServiceGrapher.Vertex;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Unit tests for {@link ServiceGrapher.Vertex}.
 *
 * @author tkral
 */
public class ServiceGrapherVertexTest {

    @DataProvider
    Object[][] equalsProvider() {
        return new Object[][] {
                // Case: Non-Vertex objects
                {new Vertex.Builder().setGuiceKey(Key.get(String.class)).build(), null, false},
                {new Vertex.Builder().setGuiceKey(Key.get(String.class)).build(), new Object(), false},
                // Case: Same key types
                {new Vertex.Builder().setGuiceKey(Key.get(String.class)).build(),
                        new Vertex.Builder().setGuiceKey(Key.get(String.class)).build(), true},
                // Case: Different key types
                {new Vertex.Builder().setGuiceKey(Key.get(String.class)).build(),
                        new Vertex.Builder().setGuiceKey(Key.get(Integer.class)).build(), false},
                // Case: Mismatched annotations
                {new Vertex.Builder().setGuiceKey(Key.get(String.class, ApplicationName.class)).build(),
                        new Vertex.Builder().setGuiceKey(Key.get(String.class)).build(), false},
                // Case: Mismatched sources
                {new Vertex.Builder().setGuiceKey(Key.get(String.class)).setSource(Provider.class).build(),
                        new Vertex.Builder().setGuiceKey(Key.get(String.class)).build(), false},
                // Case: Same key types, same annotations, same sources
                {new Vertex.Builder().setGuiceKey(Key.get(String.class, ApplicationName.class)).setSource(Provider.class).build(),
                        new Vertex.Builder().setGuiceKey(Key.get(String.class, ApplicationName.class)).setSource(Provider.class).build(), true},
        };
    }

    @Test(dataProvider = "equalsProvider")
    public void testEquals(final Vertex vertex, final Object obj, final boolean expectedResult) {
        assertThat(vertex.equals(obj), is(expectedResult));
    }

    @Test(dataProvider = "equalsProvider")
    public void testHashCode(final Vertex vertex, final Object obj, final boolean expectedResult) {
        if (obj != null && obj instanceof Vertex) {
            final Matcher<Integer> hashCodeMatcher = expectedResult ? is(vertex.hashCode()) : not(vertex.hashCode());
            assertThat(obj.hashCode(), hashCodeMatcher);
        }
    }

    @Test
    public void testInHashSet() {
        final HashSet<Vertex> vertexSet = new HashSet<>();
        vertexSet.add(new Vertex.Builder().setGuiceKey(Key.get(String.class)).build());
        vertexSet.add(new Vertex.Builder().setGuiceKey(Key.get(String.class)).build());

        assertThat(vertexSet, hasSize(1));
    }
}
