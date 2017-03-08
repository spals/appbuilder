package net.spals.appbuilder.app.core.grapher.ascii;

import com.google.inject.Key;
import net.spals.appbuilder.app.core.grapher.ServiceGrapher.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link AsciiServiceGrapher}
 *
 * @author tkral
 */
public class AsciiServiceGrapherTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsciiServiceGrapherTest.class);

    @DataProvider
    Object[][] isOrphanProvider() {
        return new Object[][] {
                // Case: Connected vertices
                {new Vertex.Builder().setGuiceKey(Key.get(String.class)).build(), false},
                {new Vertex.Builder().setGuiceKey(Key.get(AsciiServiceGrapherTest.class)).build(), false},
                // Case: Unconnected vertex
                {new Vertex.Builder().setGuiceKey(Key.get(Integer.class)).build(), true},
                // Case: Unknown vertex
                {new Vertex.Builder().setGuiceKey(Key.get(Double.class)).build(), true},
        };
    }

    @Test(dataProvider = "isOrphanProvider")
    public void testIsOrphan(final Vertex vertex, final boolean expectedResult) {
        final AsciiServiceGrapher serviceGrapher = new AsciiServiceGrapher(LOGGER);

        final Vertex vertex1 = new Vertex.Builder().setGuiceKey(Key.get(String.class)).build();
        final Vertex vertex2 = new Vertex.Builder().setGuiceKey(Key.get(AsciiServiceGrapherTest.class)).build();
        final Vertex vertex3 = new Vertex.Builder().setGuiceKey(Key.get(Integer.class)).build();

        serviceGrapher.addVertex(vertex1).addVertex(vertex2).addVertex(vertex3).addEdge(vertex1, vertex2);
        assertThat(serviceGrapher.isOrphan(vertex), is(expectedResult));
    }
}
