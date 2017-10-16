package net.spals.appbuilder.graph.model;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createVertex;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createVertexWithProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link PrintableVertex}.
 *
 * @author tkral
 */
public class PrintableVertexTest {

    @Test
    public void tetGetGuiceKey() {
        final ServiceDAGVertex vertex = Mockito.mock(ServiceDAGVertex.class);
        final PrintableVertex printableVertex = new PrintableVertex(vertex, " ");

        printableVertex.getGuiceKey();
        verify(vertex).getGuiceKey();
    }

    @Test
    public void tetGetServiceInstance() {
        final ServiceDAGVertex vertex = Mockito.mock(ServiceDAGVertex.class);
        final PrintableVertex printableVertex = new PrintableVertex(vertex, " ");

        printableVertex.getServiceInstance();
        verify(vertex).getServiceInstance();
    }

    @Test
    public void tetGetProviderSource() {
        final ServiceDAGVertex vertex = Mockito.mock(ServiceDAGVertex.class);
        final PrintableVertex printableVertex = new PrintableVertex(vertex, " ");

        printableVertex.getProviderSource();
        verify(vertex).getProviderSource();
    }

    @DataProvider
    Object[][] toStringProvider() {
        return new Object[][] {
            {createVertex(Key.get(String.class), "1"), " ", "\"1\""},
            {createVertex(Key.get(String.class, Names.named("constant")), "1"), "~",
                "@Named(constant)~\"1\""},
            {createVertexWithProvider(createVertex(Key.get(PrintableVertexTest.class), new PrintableVertexTest()),
                createVertex(Key.get(Provider.class), new Provider<PrintableVertexTest>() {
                    @Override
                    public PrintableVertexTest get() {
                        return new PrintableVertexTest();
                    }
                })), "~",
                "PrintableVertexTest~[Provider:Provider]"},

        };
    }

    @Test(dataProvider = "toStringProvider")
    public void testToString(final ServiceDAGVertex<?> vertex,
                             final String separator,
                             final String expectedResult) {
        final PrintableVertex printableVertex = new PrintableVertex(vertex, separator);
        assertThat(printableVertex.toString(), is(expectedResult));
    }
}
