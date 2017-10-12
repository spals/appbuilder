package net.spals.appbuilder.graph.model;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static net.spals.appbuilder.graph.model.ServiceDAGVertex.newVertex;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.vertexWithProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link PrintableGraphVertex}.
 *
 * @author tkral
 */
public class PrintableServiceVertexTest {

    @Test
    public void tetGetGuiceKey() {
        final ServiceDAGVertex vertex = Mockito.mock(ServiceDAGVertex.class);
        final PrintableGraphVertex printableVertex = new PrintableGraphVertex(vertex, " ");

        printableVertex.getGuiceKey();
        verify(vertex).getGuiceKey();
    }

    @Test
    public void tetGetServiceInstance() {
        final ServiceDAGVertex vertex = Mockito.mock(ServiceDAGVertex.class);
        final PrintableGraphVertex printableVertex = new PrintableGraphVertex(vertex, " ");

        printableVertex.getServiceInstance();
        verify(vertex).getServiceInstance();
    }

    @Test
    public void tetGetProviderSource() {
        final ServiceDAGVertex vertex = Mockito.mock(ServiceDAGVertex.class);
        final PrintableGraphVertex printableVertex = new PrintableGraphVertex(vertex, " ");

        printableVertex.getProviderSource();
        verify(vertex).getProviderSource();
    }

    @DataProvider
    Object[][] toStringProvider() {
        return new Object[][] {
            {newVertex(Key.get(String.class), "1"), " ", "\"1\""},
            {newVertex(Key.get(String.class, Names.named("constant")), "1"), "~",
                "@Named(constant)~\"1\""},
            {vertexWithProvider(newVertex(Key.get(PrintableServiceVertexTest.class), new PrintableServiceVertexTest()),
                newVertex(Key.get(Provider.class), new Provider<PrintableServiceVertexTest>() {
                    @Override
                    public PrintableServiceVertexTest get() {
                        return new PrintableServiceVertexTest();
                    }
                })), "~",
                "PrintableServiceVertexTest~[Provider:Provider]"},

        };
    }

    @Test(dataProvider = "toStringProvider")
    public void testToString(final ServiceDAGVertex<?> vertex,
                             final String separator,
                             final String expectedResult) {
        final PrintableGraphVertex printableVertex = new PrintableGraphVertex(vertex, separator);
        assertThat(printableVertex.toString(), is(expectedResult));
    }
}
